package nfn

import akka.actor.{Actor, ActorRef, Kill, PoisonPill}
import akka.event.Logging
import akka.pattern.ask
import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet.{CCNName, Content, MetaInfo}
import config.StaticConfig
import nfn.ComputeWorker._
import nfn.service._

import scala.concurrent.{Await, CancellationException, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ComputeWorker {
  case class Callable(callable: CallableNFNService)
  case class End()
}

class e extends Throwable

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
    if (computeName.isCompute && computeName.isNFN) {
      logger.debug(s"Received compute request, creating callable for: $computeName")
      val rawComputeName = computeName.withoutCompute.withoutThunk.withoutNFN
      assert(rawComputeName.cmps.size == 1, "Compute cmps at this moment should only have one component")

      val futCallableServ: Future[CallableNFNService] = NFNService.parseAndFindFromName(rawComputeName.cmps.head, ccnServer)
      // send back thunk content when callable service is creating (means everything was available)
      if (useThunks) {
        futCallableServ foreach { callableServ =>
          // TODO: No default value for default time estimate
          requestor ! Content(computeName, callableServ.executionTimeEstimate.fold("")(_.toString).getBytes, MetaInfo.empty)
        }
      }
      maybeFutCallable = Some(futCallableServ)
      maybeFutCallable
    } else {
      logger.error(s"Dropping compute interest $computeName, because it does not begin with ${CCNName.computeKeyword}, end with ${CCNName.nfnKeyword} or is not a thunk, therefore is not a valid compute interest")
      None
    }
  }

  def resultDataOrRedirect(data: Array[Byte], name: CCNName, ccnServer: ActorRef): Future[Array[Byte]] = {
    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
      name.expression match {
        case Some(expr) =>
          val redirectName = nodePrefix.cmps ++ List(expr)

          val content = Content(CCNName(redirectName, None), data)
          implicit val timeout = StaticConfig.defaultTimeout
          ccnServer ? NFNApi.AddToCCNCache(content) map {
            case NFNApi.AddToCCNCacheAck(_) =>
              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectName)
              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
              logger.debug(s"received AddToCacheAck, returning redirect result $redirectResult")
              redirectResult.getBytes
            case answer @ _ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
          }
        case None => throw new Exception(s"Name $name could not be transformed to an expression")
      }
    } else {
      val fut = Future(data)
      fut
    }
  }

  def executeCallable(futCallable: Future[CallableNFNService], name: CCNName, senderCopy: ActorRef): Unit = {
    futCallable foreach { callable =>
      val cancellable = KlangCancellableFuture {
        try {
          val resultValue: NFNValue = callable.exec
          val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
          val resultData = Await.result(futResultData, 0 nanos)
          Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
        } catch {
          case e: Exception =>
            println(s"Catched exception: $e")
        }
      }
      cancellable onComplete {
        case Success(content) => {
          println("success")
          logger.info(s"Finished computation, result: $content")
          senderCopy ! content
        }
        case Failure(ex) => {
          println("failure")
          logger.error(ex, s"Error when executing the service $name. Cause: ${ex.getCause} Message: ${ex.getMessage}")
        }
      }
      cancellable.cancel()
    }

//    futCallable flatMap { callable =>
//      val resultValue: NFNValue = callable.exec
//      val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
//      futResultData map { resultData =>
//        Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
//      }
//    } onComplete {
//      case Success(content) => {
//        logger.info(s"Finished computation, result: $content")
//        senderCopy ! content
//      }
//      case Failure(ex) => {
//        logger.error(ex, s"Error when executing the service $name. Cause: ${ex.getCause} Message: ${ex.getMessage}")
//      }
//    }
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
          // Compute request was sent directly without a Thunk message
          // This means we can prepare the callable by directly invoking receivedComputeRequest
          prepareCallable(name, useThunks = false, senderCopy) match {
            case Some(futCallable) => {
              executeCallable(futCallable, name, senderCopy)
            }
            case None => logger.warning(s"Could not prepare a callable for name $name")
          }
      }
    }
    case ComputeWorker.End() => {
      logger.info("Received End message")
      context.stop(self)
//      self ! PoisonPill
    }

  }
}
