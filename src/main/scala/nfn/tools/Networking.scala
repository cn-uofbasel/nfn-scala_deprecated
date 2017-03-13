package nfn.tools

import java.util.concurrent.TimeoutException

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet.{CCNName, Content, Interest, MetaInfo, _}
import akka.actor.Status._
import akka.event.Logging
import akka.util.Timeout
import ccn.ccnlite.CCNLiteInterfaceCli
import config.StaticConfig
import nfn.NFNApi
import nfn.service._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
 * Created by blacksheeep on 16/11/15.
 */
object Networking {
  /**
   * Try to fetch content object by given interest.
   *
   * @param    interest   Interest to send out
   * @param    ccnApi     Actor Reference
   * @param    time       Timeout
   * @return              Content Object (on success)
   */
  def fetchContent(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    // try to fetch data and return if successful
    val futServiceContent: Future[Content] = loadFromCacheOrNetwork(interest)
    Await.result(futServiceContent, time) match {
      case c: Content => Some(c)
      case _ => None  // send keepalive interest
    }
  }

  def fetchContentRepeatedly(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    while (true) {
      val futContent = loadFromCacheOrNetwork(interest)
      try {
        val result = Await.result(futContent, time) match {
          case c: Content => Some(c)
          case _ => None
        }
        if (result.isDefined)
          return result
      } catch {
        case e: TimeoutException => println("timeout, retry")
      }
    }
    None
  }

  def fetchContentAndKeepAlive(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    def keepaliveInterest(interest: Interest): Interest = Interest(interest.name.makeKeepaliveName)

//    println("fetchContentAndKeepalive")

    val futServiceContent = loadFromCacheOrNetwork(interest)
    try {
      val result = Await.result(futServiceContent, time) match {
        case c: Content => Some(c)
        case _ => None  // send keepalive interest
      }
      if (result.isDefined)
        return result
    } catch {
      case e: TimeoutException => println ("timeout")
    }

    var keepTrying = true
    while (keepTrying) {
      val futContent = loadFromCacheOrNetwork(interest)
      val futKeepalive = loadFromCacheOrNetwork(keepaliveInterest(interest))

      try {
        val result = Await.result(futContent, time) match {
          case c: Content => Some(c)
          case _ => None
        }
        if (result.isDefined)
          return result
      } catch {
        case e: TimeoutException => println("timeout")
      }

      keepTrying = futKeepalive.value.isDefined
    }
    None
  }

  /**
   * Try to fetch content object by name.
   *
   * @param    name       Name
   * @param    ccnApi     Actor
   * @param    time       Timeout
   * @return              Content Object (on success)
   */
  def fetchContent(name: String, ccnApi: ActorRef, time: Duration): Option[Content] = {
    val i = Interest(CCNName(name.split("/").tail: _*))
    fetchContent(i, ccnApi, time)
  }
//
//  def intermediateDataOrRedirect(ccnApi: ActorRef, name: CCNName, data: Array[Byte]): Future[Array[Byte]] = {
//    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
//      name.expression match {
//        case Some(expr) =>
//          val redirectName = name.cmps
//          val content = Content(CCNName(redirectName, None), data)
//          implicit val timeout = StaticConfig.defaultTimeout
//          ccnApi ? NFNApi.AddToCCNCache(content) map {
//            case NFNApi.AddToCCNCacheAck(_) =>
//              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectName)
//              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
//              redirectResult.getBytes
//            case answer @ _ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
//          }
//        case None => throw new Exception(s"Name $name could not be transformed to an expression")
//      }
//    } else Future(data)
//  }

  def intermediateResult(ccnApi: ActorRef, name: CCNName, count: Int, resultValue: NFNValue) = {
    var contentName = name
//    println("Content Name: " + contentName.toString)

    contentName = contentName.append(CCNName.requestKeyword)
    contentName = contentName.append(s"${CCNName.getIntermediateKeyword} ${count.toString}")
//    contentName = contentName.append(count.toString)
//    println("Content Name: " + contentName.toString)

    contentName = contentName.withCompute
//    println("Content Name: " + contentName.toString)

    contentName = contentName.withNFN
    println("Content Name: " + contentName.toString)



//    val futIntermediateData = intermediateDataOrRedirect(ccnApi, contentName, resultValue.toDataRepresentation)
//    futIntermediateData map {
//      resultData => Content(contentName, resultData, MetaInfo.empty)
//    } onComplete {
//      case Success(content) => {
//        ccnApi ! NFNApi.AddToCCNCache(content)
//      }
//      case Failure(ex) => {
//
//      }
//    }

    val content = Content(contentName, resultValue.toDataRepresentation)
    ccnApi ! NFNApi.AddIntermediateResult(content)


//    ccnApi ! NFNApi.AddToCCNCache(Content(contentName, resultValue.toDataRepresentation))



//    resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)

//    futCallable flatMap { callable =>
//      val resultValue: NFNValue = callable.exec
//      val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
//      futResultData map { resultData =>
//        Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
//
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
}
