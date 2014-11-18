package nfn

import java.net.InetSocketAddress

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

  case class AddToLocalCache(content: Content, prependLocalPrefix: Boolean = false)

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

object UDPConnectionContentInterest {
  def apply(context: ActorRefFactory, from: InetSocketAddress, to: InetSocketAddress, ccnIf: CCNInterface): ActorRef = {
    context.actorOf(
      Props(
        classOf[UDPConnectionContentInterest],
        from,
        to,
        ccnIf
      ),
      name = s"udpsocket-${from.getHostName}:${from.getPort}-${to.getHostName}:${to.getPort}"
    )
  }
}


class UDPConnectionContentInterest(local:InetSocketAddress,
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

  def handlePacket(packet: CCNPacket, senderCopy: ActorRef) =
    packet match {
      case i: Interest =>
        ccnLite.mkBinaryInterest(i) onComplete {
          case Success(binaryInterest) =>
            self.tell(UDPConnection.Send(binaryInterest), senderCopy)
          case Failure(e) => logger.error(e, s"could not create binary interest for $i")
        }
      case c: Content =>
        ccnLite.mkBinaryContent(c) onComplete {
          case Success(binaryContents) => {
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
case class NFNServer(nfnNodeConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig, ccnIf: CCNInterface) extends Actor {

  implicit val execContext = context.dispatcher


  val logger = Logging(context.system, this)
  logger.debug(s"self: $self")

//  nfnNodeConfig.config
//  val ccnIf = new CCNLiteInterfaceWrapper()

  val cacheContent: Boolean = true

  val computeServer: ActorRef = context.actorOf(Props(classOf[ComputeServer]), name = "ComputeServer")

  val maybeLocalAbstractMachine: Option[ActorRef] =
      if(computeNodeConfig.withLocalAM)
        Some(context.actorOf(Props(classOf[LocalAbstractMachineWorker], self), "LocalAM"))
      else None

  val defaultTimeoutDuration = StaticConfig.defaultTimeoutDuration

  var pit: ActorRef = context.actorOf(Props(classOf[PIT]), name = "PIT")
  val cs = ContentStore()

  val nfnGateway: ActorRef =
    UDPConnectionContentInterest(
      context.system,
      new InetSocketAddress(computeNodeConfig.host, computeNodeConfig.port),
      new InetSocketAddress(nfnNodeConfig.host, nfnNodeConfig.port),
      ccnIf
    )


  override def preStart() = {
      nfnGateway ! UDPConnection.Handler(self)
  }


  private def handleContentChunk(contentChunk: Content, senderCopy: ActorRef) = {
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

      implicit val timeout = Timeout(timeoutFromContent)
      (pit ? PIT.Get(content.name)).mapTo[Option[Set[Face]]] onSuccess {
          case Some(pendingFaces) => {
            val (contentNameWithoutThunk, isThunk) = content.name.withoutThunkAndIsThunk

            assert(isThunk, s"handleInterstThunkContent received the content object $content which is not a thunk")

            val interest = Interest(contentNameWithoutThunk)
            logger.debug(s"Received usethunk $content, sending actual interest $interest")

            logger.debug(s"Timeout duration: ${timeout.duration}")
            pendingFaces foreach { pf => pit ! PIT.Add(contentNameWithoutThunk, pf, timeout.duration) }
            nfnGateway ! interest
              // else it was a thunk interest, which means we can now send the actual interest
            pit ! PIT.Remove(content.name)
          }
          case None => logger.error(s"Discarding thunk content $content because there is no entry in pit")
        }
    }
    def handleNonThunkContent: Unit = {
      implicit val timeout = Timeout(defaultTimeoutDuration)
      (pit ? PIT.Get(content.name)).mapTo[Option[Set[Face]]] onSuccess {
        case Some(pendingFaces) => {

          if (cacheContent) {
            cs.add(content)
          }

          pendingFaces foreach { pendingSender => pendingSender.send(content) }

          pit ! PIT.Remove(content.name)
        }
        case None =>
          logger.warning(s"Discarding content $content because there is no entry in pit")
      }
    }
  }


  private def handleInterest(i: Interest, senderCopy: ActorRef) = {

    implicit val timeout = Timeout(defaultTimeoutDuration)
    cs.get(i.name) match {
      case Some(contentFromLocalCS) =>
        logger.debug(s"Served $contentFromLocalCS from local CS")
        senderCopy ! contentFromLocalCS
      case None => {
        val senderFace = ActorRefFace(senderCopy)
        (pit ? PIT.Get(i.name)).mapTo[Option[Set[Face]]] onSuccess {
          case Some(pendingFaces) =>
            pit ! PIT.Add(i.name, senderFace, defaultTimeoutDuration)
          case None => {
            pit ! PIT.Add(i.name, senderFace, defaultTimeoutDuration)

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
                if(i.name.isThunk) {
                  computeServer ! ComputeServer.Thunk(i.name)
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
      }
    }
  }

  def handleNack(nack: Nack, senderCopy: ActorRef) = {
    if(StaticConfig.isNackEnabled) {
      implicit val timeout = Timeout(defaultTimeoutDuration)
      (pit ? PIT.Get(nack.name)).mapTo[Option[Set[Face]]] onSuccess {
        case Some(pendingFaces) => {
          pendingFaces foreach {
            _.send(nack)
          }
          pit ! PIT.Remove(nack.name)
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
        logger.info(s"Received interest: $i")
        handleInterest(i, senderCopy)
      }
      case c: Content => {
        logger.info(s"Received content: $c")
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
      val maybeThunkInterest =
        if(interest.name.isNFN && useThunks) interest.thunkify
        else interest
      handlePacket(maybeThunkInterest, senderCopy)
    }

    case NFNApi.AddToCCNCache(content) => {
      logger.info(s"creating add to cache messages for $content")
      ccnIf.mkAddToCacheInterest(content) onComplete {
        case Success(binaryAddToCacheReqs) =>
          logger.debug(s"sending ${binaryAddToCacheReqs.size} add to cache requests for ${content.name} to the network")
          binaryAddToCacheReqs foreach { binaryAddToCacheReq =>
            nfnGateway ! UDPConnection.Send(binaryAddToCacheReq)
          }
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


    case Exit() => {
      exit()
      context.system.shutdown()
    }
  }

  def exit(): Unit = {
    computeServer ! PoisonPill
    nfnGateway ! PoisonPill
  }
}
