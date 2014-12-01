package nfn

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet.{CCNName, Content, MetaInfo}
import nfn.ComputeWorker._
import nfn.service.{CallableNFNService, NFNService, NFNValue}
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ComputeWorker {
  case class Callable(callable: CallableNFNService)
  case class End()
}

case class ComputeWorker(ccnServer: ActorRef, nodePrefix: CCNName) extends Actor {
  import context.dispatcher

  val logger = Logging(context.system, this)

  var maybeFutCallable: Option[Future[CallableNFNService]] = None

  def receivedContent(content: Content) = {
    // Received content from request (sendrcv)
    logger.error(s"ComputeWorker received content, discarding it because it does not know what to do with it")
  }

  // Received compute request
  // Make sure it actually is a compute request and forward to the handle method
  def prepareCallable(computeName: CCNName, useThunks: Boolean, requestor: ActorRef): Option[Future[CallableNFNService]] = {
    if(computeName.isCompute && computeName.isNFN) {
      logger.debug(s"Received compute request: $computeName")
      val computeCmps = computeName.withoutCompute.withoutThunk.withoutNFN
      handleComputeRequest(computeCmps, computeName, useThunks, requestor)
    } else {
      logger.error(s"Dropping compute interest $computeName, because it does not begin with ${CCNName.computeKeyword}, end with ${CCNName.nfnKeyword} or is not a thunk, therefore is not a valid compute interest")
      None
    }
  }


  /*
   * Parses the compute request and instantiates a callable service.
   * On success, sends a thunk back if required, executes the services and sends the result back.
   */
  def handleComputeRequest(computeName: CCNName, originalName: CCNName, useThunks:Boolean, requestor: ActorRef): Option[Future[CallableNFNService]] = {
    logger.debug(s"Handling compute request for name: $computeName")
    assert(computeName.cmps.size == 1, "Compute cmps at this moment should only have one component")
    val futCallableServ: Future[CallableNFNService] = NFNService.parseAndFindFromName(computeName.cmps.head, ccnServer)


    // send back thunk content when callable service is creating (means everything was available)
    if(useThunks) {
      futCallableServ foreach { callableServ =>
        // TODO: No default value for default time estimate
        requestor ! Content(originalName, callableServ.executionTimeEstimate.fold("")(_.toString).getBytes, MetaInfo.empty)
      }
    }
    maybeFutCallable = Some(futCallableServ)
    maybeFutCallable
  }

  def resultDataOrRedirect(data: Array[Byte], name: CCNName, ccnServer: ActorRef): Future[Array[Byte]] = {
    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
      name.expression match {
        case Some(expr) =>
          val redirectName = nodePrefix.cmps ++ List(expr)
          ccnServer ! NFNApi.AddToCCNCache(Content(CCNName(redirectName, None), data))

          Thread.sleep(1000)

          val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectName)
          val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
          Future(redirectResult.getBytes)
        case None => throw new Exception(s"Name $name could not be transformed to an expression")
      }
    } else Future(data)
  }

  def executeCallable(futCallable: Future[CallableNFNService], name: CCNName, senderCopy: ActorRef) = {
    futCallable flatMap { callable =>
      val resultValue: NFNValue = callable.exec
      val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
      futResultData map { resultData =>

        Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)

      }
    } onComplete {
      case Success(content) => {
        logger.info(s"Finished computation, result: $content")
        senderCopy ! content
      }
      case Failure(ex) => {
        logger.error(ex, s"Error when executing the service $name.")
      }
    }
  }

  override def receive: Actor.Receive = {
    case ComputeServer.Thunk(name) => {
      prepareCallable(name, useThunks = true, sender)
    }
    case msg @ ComputeServer.Compute(name) => {
      val senderCopy = sender
      maybeFutCallable match {
        case Some(futCallable) => {
          executeCallable(futCallable, name, senderCopy)
        }
        case None =>
          // Compute request was send directly without a Thunk message
          // This means we can prepare the callable by direclty invoking receivedComputeRequest
          prepareCallable(name, useThunks = false, senderCopy) match {
            case Some(futCallable) => executeCallable(futCallable, name, senderCopy)
            case None => logger.warning(s"Could not prepare a callable for name $name")
          }
      }
    }
    case End() => context.stop(self)
  }
}
