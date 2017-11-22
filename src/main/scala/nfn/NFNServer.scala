package nfn

import java.net.{InetAddress, InetSocketAddress}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout

import ccn._
import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet._
import com.typesafe.scalalogging.slf4j.Logging
import config.{ComputeNodeConfig, RouterConfig, StaticConfig}
import monitor.Monitor
import monitor.Monitor.PacketLogWithoutConfigs
import network._
import nfn.NFNServer._
import nfn.localAbstractMachine.LocalAbstractMachineWorker



object NFNServer {


  case class ComputeResult(content: Content)

  case class Exit()
}

object NFNApi {

  case class CCNSendReceive(interest: Interest, useThunks: Boolean)

  case class AddToCCNCache(content: Content)
  case class AddToCCNCacheAck(name: CCNName)

  case class AddToLocalCache(content: Content, prependLocalPrefix: Boolean = false)
  case class GetFromLocalCache(interest: Interest)

  case class AddIntermediateResult(content: Content)
}


object NFNServerFactory extends Logging {
  def nfnServer(context: ActorRefFactory, nfnRouterConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig) = {


    val wireFormat = StaticConfig.packetformat

    implicit val execContext = context.dispatcher
    val ccnLiteIf: CCNInterface = CCNLiteInterfaceCli(wireFormat)

    context.actorOf(networkProps(nfnRouterConfig, computeNodeConfig, ccnLiteIf), name = "NFNServer")
  }
  def networkProps(nfnNodeConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig, ccnIf: CCNInterface) =
    Props(classOf[NFNServer], nfnNodeConfig, computeNodeConfig, ccnIf)
}

object UDPConnectionWireFormatEncoder {
  def apply(context: ActorRefFactory, from: InetSocketAddress, to: InetSocketAddress, ccnIf: CCNInterface): ActorRef = {
    context.actorOf(
      Props(
        classOf[UDPConnectionWireFormatEncoder],
        from,
        to,
        ccnIf
      ),
      name = s"udpsocket-${from.getPort}-${to.getPort}"
    )
  }
}

// This class takes out some work from the NFN server.
// It encodes each packet send to the network to the wireformat and it also logs all send messages
class UDPConnectionWireFormatEncoder(local:InetSocketAddress,
                                   target:InetSocketAddress,
                                   ccnLite: CCNInterface) extends UDPConnection(local, Some(target)) {


  implicit val execContext = context.dispatcher
  def logPacket(packet: CCNPacket) = {
    val maybePacketLog = packet match {
      case i: Interest => Some(Monitor.InterestInfoLog("interest", i.name.toString))
      case c: Content => Some(Monitor.ContentInfoLog("content", c.name.toString, c.possiblyShortenedDataString))
      case n: Nack => Some(Monitor.ContentInfoLog("content", n.name.toString, ":NACK"))
      case a: AddToCacheAck => None // Not Monitored for now
      case a: AddToCacheNack => None // Not Monitored for now
    }

    maybePacketLog map { packetLog =>
      Monitor.monitor ! new PacketLogWithoutConfigs(local.getHostString, local.getPort, target.getHostString, target.getPort, true, packetLog)
    }
  }

  def handlePacket(packet: CCNPacket, senderCopy: ActorRef) = {
    packet match {
      case i: Interest =>
        logger.debug(s"handling interest: $packet")
        ccnLite.mkBinaryInterest(i) onComplete {
          case Success(binaryInterest) =>
            logger.debug(s"Sending binary interest for $i to network")
            self.tell(UDPConnection.Send(binaryInterest), senderCopy)
          case Failure(e) => logger.error(e, s"could not create binary interest for $i")
        }
      case c: Content =>
        ccnLite.mkBinaryContent(c) onComplete {
          case Success(binaryContents) => {
            logger.debug(s"Sending ${binaryContents} binary content objects for $c to network")
            binaryContents foreach { binaryContent =>
              self.tell(UDPConnection.Send(binaryContent), senderCopy)
            }
          }
          case Failure(e) => logger.error(e, s"could not create binary content for $c")
        }
      case n: Nack =>
        ccnLite.mkBinaryContent(Content(n.name, n.content.getBytes, MetaInfo.empty)) onComplete {
          case Success(binaryContents) => {
            binaryContents foreach { binaryContent =>
              self.tell(UDPConnection.Send(binaryContent), senderCopy)
            }
          }
          case Failure(e) => logger.error(e, s"could not create binary nack for $n")
        }
      case a: AddToCacheAck =>
        logger.warning("received AddToCacheAck to send to a UDPConnection, dropping it")
      case a: AddToCacheNack =>
        logger.warning("received AddToCacheNack to send to a UDPConnection, dropping it!")
    }
  }

  def interestContentReceiveWithoutLog: Receive = {
    case p: CCNPacket => {
      val senderCopy = sender
      handlePacket(p, senderCopy)
    }
  }

  def interestContentReceive: Receive = {
    case p: CCNPacket => {
      logPacket(p)
      val senderCopy = sender
      handlePacket(p, senderCopy)
    }
  }

  override def receive = super.receive orElse interestContentReceiveWithoutLog

  override def ready(actorRef: ActorRef) = super.ready(actorRef) orElse interestContentReceive
}

/**
 * The NFNServer is the gateway interface to the CCNNetwork and provides the NFNServer implements the [[NFNApi]].
 * It manages a localAbstractMachine cs form where any incoming content requests are served.
 * It also maintains a pit for received interests. Since everything is akka based, the faces in the pit are [[ActorRef]],
 * this means that usually these refs are to:
 *  - interest from the network with the help of [[UDPConnection]] (or any future connection type)
 *  - the [[ComputeServer]] or [[ComputeWorker]]can make use of the pit
 *  - any interest with the help of the akka "ask" pattern
 *  All connection, interest and content request are logged to the [[Monitor]].
 *  A NFNServer also maintains a socket which is connected to the actual CCNNetwork, usually an CCNLiteInterfaceWrapper instance encapsulated in a [[CCNLiteProcess]].
 */
//case class NFNServer(maybeNFNNodeConfig: Option[RouterConfig], maybeComputeNodeConfig: Option[ComputeNodeConfig]) extends Actor {
case class NFNServer(routerConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig, ccnIf: CCNInterface) extends Actor {

  implicit val execContext = context.dispatcher

  val logger = Logging(context.system, this)

  val cacheContent: Boolean = true

  val computeServer: ActorRef = context.actorOf(Props(classOf[ComputeServer], computeNodeConfig.prefix), name = "ComputeServer")

  val maybeLocalAbstractMachine: Option[ActorRef] =
    if(computeNodeConfig.withLocalAM)
      Some(context.actorOf(Props(classOf[LocalAbstractMachineWorker], self), "LocalAM"))
    else None

  val defaultTimeoutDuration = StaticConfig.defaultTimeoutDuration

  var pit: PIT = PIT(context)
  val cs = ContentStore()

  val nfnGateway: ActorRef =
    UDPConnectionWireFormatEncoder(
      context.system,
      new InetSocketAddress(computeNodeConfig.host, computeNodeConfig.port),
      new InetSocketAddress(routerConfig.host, routerConfig.port),
      ccnIf
    )


  override def preStart() = {
    nfnGateway ! UDPConnection.Handler(self)
  }


  private def handleContentChunk(contentChunk: Content, senderCopy: ActorRef): Unit = {
    logger.debug("enter handleContentChunk")

    val maybeFace = pit.get(contentChunk.name.withou3tChunk)
    if (maybeFace.isEmpty) {
      logger.error(s"content ${contentChunk.name} not found in PIT")
      return
    }
    val face: Set[ActorRef] = maybeFace match {case Some(f) => f}

    cs.add(contentChunk)
    cs.getContentCompleteOrIncompletedChunks(contentChunk.name) match {
      case Left(content) =>
        logger.debug(s"unchunkified content $content")
        handleContent(content, senderCopy)
      case Right(chunkNums) => {
        chunkNums match {
          case chunkNum :: _ =>
            val chunkInterest = Interest(CCNName(contentChunk.name.cmps, Some(chunkNum)))
            self ! NFNApi.CCNSendReceive(chunkInterest, contentChunk.name.isThunk)
          case _ => logger.warning(s"chunk store was already removed or never existed in contentstore for contentname ${contentChunk.name}")
        }
      }
    }
    pit.get(contentChunk.name) match {
//      case Some(name) => {pit.add}
      case _ => face foreach {
        pit.add(contentChunk.name, _, 10 seconds)
      }
    }
  }

  private def handleContent(content: Content, senderCopy: ActorRef) = {

    if(content.name.isThunk && !content.name.isCompute) {
      handleInterstThunkContent
    } else {
      handleNonThunkContent
    }

    def handleInterstThunkContent: Unit = {
      def timeoutFromContent: FiniteDuration = {
        val timeoutInContent = new String(content.data)
        if(timeoutInContent != "" && timeoutInContent.forall(_.isDigit)) {
          timeoutInContent.toInt.seconds
        } else {
          defaultTimeoutDuration
        }
      }


      pit.get(content.name) match {
        case Some(pendingFaces) => {
          val (contentNameWithoutThunk, isThunk) = content.name.withoutThunkAndIsThunk

          assert(isThunk, s"handleInterestThunkContent received the content object $content which is not a thunk")

          val interest = Interest(contentNameWithoutThunk)
          logger.debug(s"Received usethunk $content, sending actual interest $interest")
          //          logger.debug(s"Timeout duration: ${timeout.duration}")
          val timeout = Timeout(timeoutFromContent)
          pendingFaces foreach { pf =>
            pit.add(contentNameWithoutThunk, pf, timeout.duration)
          }

          nfnGateway ! interest
          pit.remove(content.name)
        }
        case None => logger.error(s"Discarding thunk content $content because there is no entry in pit")
      }
    }

    def handleNonThunkContent: Unit = {
      //FIXME: Version hack for Openmhealth
      val cname = if(content.name.cmps.head == "org" && content.name.cmps.tail.head == "openmhealth" && content.name.cmps.contains("catalog"))
        CCNName(content.name.cmps.reverse.tail.reverse, None) else  content.name
        println(pit.toString())
        pit.get(cname) match {
      //FIXME: End of the hack for Openmhealth
      //pit.get(content.name) match { //FIXME: if hack for Openmhealth is removed, uncomment this!
        case Some(pendingFaces) => {
          val isCountIntermediates = content.name.isRequest && content.name.requestType == "CIM"
          if (cacheContent && !content.name.isKeepalive && !isCountIntermediates) {
            cs.add(content)
          }

          val redirect = "redirect:".getBytes

          // Check if content is a redirect
          // if it is a redirect, send an interest for each pending face with the redirect name
          // otherwise return the ocntent object to all pending faces
          if(!content.name.isCompute && content.data.startsWith(redirect)) {

            val nameCmps: List[String] = new String(content.data).split("redirect:")(1).split("/").tail.toList

            val unescapedNameCmps = CCNLiteInterfaceCli.unescapeCmps(nameCmps)

            logger.info(s"Redirect for $unescapedNameCmps")
            implicit val timeout = Timeout(defaultTimeoutDuration)
            (self ? NFNApi.CCNSendReceive(Interest(CCNName(unescapedNameCmps, None)), useThunks = false)).mapTo[CCNPacket] map {
              case c: Content => {
                pendingFaces foreach { pendingFace => pendingFace ! c }
                pit.remove(content.name)
              }
              case nonContent @ _ =>
                logger.warning(s"Received $nonContent when fetching a redirected content, dropping it")
            }
          } else {
            pendingFaces foreach { pendingFace => pendingFace ! content }
            pit.remove(content.name)
          }

        }
        case None =>
          logger.warning(s"Discarding content $content because there is no entry in pit")
      }
    }
  }


  private def handleInterest(i: Interest, senderCopy: ActorRef) = {
//    if (i.name.isKeepalive) {
//      logger.debug(s"Receive keepalive interest: " + i.name)
//      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 2, Nil, 1)
//      val nfnName = i.name.copy(cmps = nfnCmps)
//      pit.get(nfnName) match {
//        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
//          senderCopy ! Content(i.name, " ".getBytes)
//        case None => logger.debug(s"Did not find in PIT.")
//      }
//    } else {
      logger.debug(s"Handle interest.")
//    logger.debug(s"Sender: $senderCopy")
      cs.get(i.name) match {
        case Some(contentFromLocalCS) =>
          logger.debug(s"Served $contentFromLocalCS from local CS")
          senderCopy ! contentFromLocalCS
        case None => {
          val senderFace = senderCopy
          pit.get(i.name) match {
            case Some(pendingFaces) => {
              if (!i.name.isRequest) {
                pit.add(i.name, senderFace, defaultTimeoutDuration)
//                nfnGateway ! i
              }
            }
            case None => {
              if (!i.name.isRequest || i.name.requestType == "CIM" || i.name.requestType == "GIM") {
                pit.add(i.name, senderFace, defaultTimeoutDuration)
              }

              // If the interest has a chunknum, make sure that the original interest (still) exists in the pit
              i.name.chunkNum foreach { _ =>
                pit.add(CCNName(i.name.cmps, None), senderFace, defaultTimeoutDuration)
              }

              // /.../.../NFN
              // nfn interests are either:
              // - send to the compute server if they start with compute
              // - send to a local AM if one exists
              // - forwarded to nfn gateway
              // not nfn interests are always forwarded to the nfn gateway
              if (i.name.isNFN) {
                // /COMPUTE/call .../.../NFN
                // A compute flag at the beginning means that the interest is a binary computation
                // to be executed on the compute server
                if (i.name.isCompute) {
                  if (i.name.isThunk) {
                    computeServer ! ComputeServer.Thunk(i.name)
                  } else if (i.name.isRequest) {
                    i.name.requestType match {
                      case "KEEPALIVE" => {
                        logger.debug(s"Receive keepalive interest: " + i.name)
                        val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 3, Nil, 2)
                        val nfnName = i.name.copy(cmps = nfnCmps)
                        pit.get(nfnName) match {
                          case Some(pendingInterest) => logger.debug(s"Found in PIT.")
                            senderCopy ! Content(i.name, " ".getBytes)
                          case None => logger.debug(s"Did not find in PIT.")
                        }
                      }
                      case "CTRL" => {
                        logger.debug(s"Receive control message: " + i.name + " Save to CS for later retrieval by computation.")
                        val emptyContent = Content(i.name, Array[Byte]())
                        cs.add(emptyContent)
                        senderCopy ! Content(i.name, " ".getBytes)
                      }
                      case _ => {
                        computeServer ! ComputeServer.RequestToComputation(i.name, senderCopy)
                      }
                    }
                  } else {
                    computeServer ! ComputeServer.Compute(i.name)
                  }

                  // /.../.../NFN
                  // An NFN interest without compute flag means that it must be reduced by an abstract machine
                  // If no local machine is available, forward it to the nfn network
                } else {
                  maybeLocalAbstractMachine match {
                    case Some(localAbstractMachine) => {
                      localAbstractMachine ! i
                    }
                    case None => {
                      nfnGateway ! i
                    }
                  }
                }
              } else {
                nfnGateway ! i
              }
            }
          }
//        }
      }
    }
  }

  def handleNack(nack: Nack, senderCopy: ActorRef) = {
    if(StaticConfig.isNackEnabled) {
      implicit val timeout = Timeout(defaultTimeoutDuration)
      pit.get(nack.name) match {
        case Some(pendingFaces) => {
          pendingFaces foreach {
            _ ! nack
          }
          pit.remove(nack.name)
        }
        case None => logger.warning(s"Received nack for name which is not in PIT: $nack")
      }
    }else{
      logger.error(s"Received nack even though nacks are disabled!")
    }
  }

  def handlePacket(packet: CCNPacket, senderCopy: ActorRef) = {
    packet match {
      case i: Interest => {
        logger.info(s"Received interest: $i (f=$senderCopy)")
        handleInterest(i, senderCopy)
      }
      case c: Content => {
        logger.info(s"Received content: $c (f=$senderCopy)")
        c.name.chunkNum match {
          case Some(chunknum) => handleContentChunk(c, senderCopy)
          case _ => handleContent(c, senderCopy)
        }
      }
      case n: Nack => {
        logger.info(s"Received NAck: $n")
        handleNack(n, senderCopy)
      }
      case a: AddToCacheAck => {
        logger.debug(s"Received AddToCacheAck")
      }
      case a: AddToCacheNack => {
        logger.error(s"Received AddToCacheNack")
      }
    }
  }

  override def receive: Actor.Receive = {
    // received Data from network
    // If it is an interest, start a compute request
    case packet:CCNPacket => {
      val senderCopy = sender
      handlePacket(packet, senderCopy)
    }
    case UDPConnection.Received(data, sendingRemote) => {
      val senderCopy = sender
      ccnIf.wireFormatDataToXmlPacket(data) onComplete {
        case Success(packet) => self.tell(packet, senderCopy)
        case Failure(ex) => logger.error(ex, "could not parse data")
      }
    }

    /**
     * [[NFNApi.CCNSendReceive]] is a message of the external API which retrieves the content for the interest and sends it back to the sender actor.
     * The sender actor can be initialized from an ask patter or form another actor.
     * It tries to first serve the interest from the localAbstractMachine cs, otherwise it adds an entry to the pit
     * and asks the network if it was the first entry in the pit.
     * Thunk interests get converted to normal interests, thunks need to be enabled with the boolean flag in the message
     */
    case NFNApi.CCNSendReceive(interest, useThunks) => {
      val senderCopy = sender

      logger.debug(s"API: Sending interest $interest (f=$senderCopy)")
      val maybeThunkInterest =
        if(interest.name.isNFN && useThunks) interest.thunkify
        else interest
      handlePacket(maybeThunkInterest, senderCopy)
    }

    case NFNApi.AddToCCNCache(content) => {
      val senderCopy = sender
      logger.info(s"creating add to cache messages for $content")
      cs.add(content)
      ccnIf.addToCache(content, routerConfig.mgmntSocket) onComplete {
        case Success(n) =>
          logger.debug(s"Send $n AddToCache requests for content $content to router ")
//          logger.debug(s"Name: ${content.name}")
          senderCopy ! NFNApi.AddToCCNCacheAck(content.name)
        case Failure(ex) => logger.error(ex, s"Could not add to CCN cache for $content")
      }
    }

    case NFNApi.AddToLocalCache(content, prependLocalPrefix) => {
      val contentToAdd =
        if(prependLocalPrefix) {
          Content(computeNodeConfig.prefix.append(content.name), content.data, MetaInfo.empty)
        } else content
      logger.info(s"Adding content for ${contentToAdd.name} to local cache")
      cs.add(contentToAdd)
    }

    case NFNApi.GetFromLocalCache(interest) => {
      val senderCopy = sender
      logger.info(s"Searching local cache for prefix ${interest.name}.")
      val contents = cs.find(interest.name)
      if (contents.isDefined) {
        cs.remove(contents.get.name)
      }
      logger.info(s"Found entries: ${contents.isDefined}")
      senderCopy ! contents
    }

    case NFNApi.AddIntermediateResult(intermediateContent) => {
      logger.info(s"Adding intermediate result for ${intermediateContent.name} to CCN cache.")
      val futIntermediateData = intermediateDataOrRedirect(self, intermediateContent.name, intermediateContent.data)
      futIntermediateData map {
        resultData => Content(intermediateContent.name, resultData, MetaInfo.empty)
      } onComplete {
        case Success(content) => {
          self ! NFNApi.AddToCCNCache(content)
        }
        case Failure(ex) => logger.error(ex, s"Could not add intermediate content to CCN cache for $intermediateContent")
      }
    }

    case Exit() => {
      exit()
      context.system.shutdown()
    }
  }

  def exit(): Unit = {
    computeServer ! PoisonPill
    nfnGateway ! PoisonPill
  }


  def intermediateDataOrRedirect(ccnApi: ActorRef, name: CCNName, data: Array[Byte]): Future[Array[Byte]] = {
    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
      name.expression match {
        case Some(expr) =>
          val cmps = computeNodeConfig.prefix.cmps ++ List(expr)
          val redirectName = CCNName(cmps, None).withIntermediate(name.intermediateIndex).withNFN
          val redirectCmps = redirectName.cmps
//          val redirectCmps
//      val redirectCmps = name.cmps
          val content = Content(redirectName, data)
          implicit val timeout = StaticConfig.defaultTimeout
          ccnApi ? NFNApi.AddToCCNCache(content) map {
            case NFNApi.AddToCCNCacheAck(_) =>
              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectCmps)
              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
              redirectResult.getBytes
            case answer @ _ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
          }
        case None => throw new Exception(s"Name $name could not be transformed to an expression")
      }
    } else Future(data)
  }
}
