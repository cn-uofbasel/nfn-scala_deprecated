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
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global



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

  def fetchContentAndKeepalive(ccnApi: ActorRef,
                               interest: Interest,
                               timeoutDuration: FiniteDuration = 20 seconds,
                               handleIntermediate: Option[(Int, Content) => Unit] = None): Option[Content] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    def keepaliveInterest(interest: Interest): Interest = Interest(interest.name.makeKeepaliveName)

    def fetchIntermediate(index: Int) = {
      println(s"Request intermediate #$interest")

      val intermediateInterest = Interest(interest.name.withRequest(s"GIM $index")) // CRASH HERE?
      val intermediateFuture = loadFromCacheOrNetwork(intermediateInterest)
      intermediateFuture onComplete {
        case Success(x) => x match {
          case c: Content => {
            println("Received intermediate content.")
            val handler = handleIntermediate.get
            handler(index, c)
          }
          case _ => println(s"Matched something else.")
        }
        case Failure(error) => println(s"Completed with error: $error")
      }
    }

    var isFirstTry = true
    var shouldKeepTrying = true
    val shouldGetIntermediates = handleIntermediate.isDefined
    val intermediateInterval = 1000

    if (shouldGetIntermediates) {
      val f = Future {
        var highestRequestedIndex = -1
        val countIntermediatesInterest = Interest(interest.name.withRequest("CIM"))

        Thread.sleep(2000)

        while (shouldKeepTrying) {
          val startTime = System.currentTimeMillis()
          val loadFuture = loadFromCacheOrNetwork(countIntermediatesInterest)
//          val timeoutFuture = Future {
//            Thread.sleep(1 * 1000)
//            throw new TimeoutException("Count intermediates timeout")
//          }
//          val future = Future.firstCompletedOf(Seq(loadFuture, timeoutFuture))
//          println("request intermediates 3")

          Await.result(loadFuture, 3 second) match {
            case c: Content =>
              if (c.data.length > 0) {
                val highestAvailableIndex = new String(c.data).toInt
                println(s"Highest available intermediate: $highestAvailableIndex")
                var index = highestRequestedIndex + 1
                val endIndex = highestAvailableIndex
                while (index <= highestAvailableIndex) {
                  fetchIntermediate(index)
                  index += 1
                }
                highestRequestedIndex = highestAvailableIndex
              } else {
                println("No intermediate results available (yet?).")
              }
            case _ => println("Loading intermediate content timed out.")
          }
          val elapsed = System.currentTimeMillis() - startTime
          if (elapsed < intermediateInterval) {
            Thread.sleep(intermediateInterval - elapsed)
          }
        }
      }
    }

    println("Fetch content and keepalive")

    while (shouldKeepTrying) {
      println("Try again.")
      val futContent = loadFromCacheOrNetwork(interest)
      val futKeepalive = if (isFirstTry) None else Some(loadFromCacheOrNetwork(keepaliveInterest(interest)))

      try {
        val result = Await.result(futContent, timeoutDuration) match {
          case c: Content => println("content."); Some(c)
          case _ => println("none."); None
        }
        if (result.isDefined)
          return result
      } catch {
        case e: TimeoutException => println("timeout")
      }

      shouldKeepTrying = isFirstTry || futKeepalive.get.value.isDefined
    }
    None
  }


  def requestContentAndKeepalive(ccnApi: ActorRef,
                                 interest: Interest,
                                 timeoutDuration: FiniteDuration = 20 seconds): Future[Option[Content]] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    def blockingRequest(): Option[Content] = {
      var isFirstTry = true
      var shouldKeepTrying = true
      while (shouldKeepTrying) {
        println("Try again.")
        val futContent = loadFromCacheOrNetwork(interest)
        val futKeepalive = if (isFirstTry) None else Some(loadFromCacheOrNetwork(Interest(interest.name.makeKeepaliveName)))

        try {
          val result = Await.result(futContent, timeoutDuration) match {
            case c: Content => println("content."); Some(c)
            case _ => println("none."); None
          }
          if (result.isDefined)
            return result
        } catch {
          case e: TimeoutException => println("timeout")
        }

        shouldKeepTrying = isFirstTry || futKeepalive.get.value.isDefined
      }
      None
    }

    Future { blockingRequest() }
  }

  def requestIntermediates(ccnApi: ActorRef,
                           interest: Interest,
                           timeoutDuration: FiniteDuration = 20 seconds,
                           handleIntermediates: (Content) => Unit): Unit = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    val f = Future {
      println("request intermediates 1")
      Thread.sleep(2 * 1000)
      println("request intermediates 2")
      var highestRequestedIndex = -1
      val countIntermediatesInterest = Interest(interest.name.withRequest("CIM"))
      val loadFuture = loadFromCacheOrNetwork(countIntermediatesInterest)
      val timeoutFuture = Future {
        Thread.sleep(3 * 1000)
        throw new TimeoutException("Future timeout")
      }
      val future = Future.firstCompletedOf(Seq(loadFuture, timeoutFuture))
      println("request intermediates 3")

      future onComplete {
        case Success(x) => x match {
          case c: Content => {
            val highestAvailableIndex = new String(c.data).toInt
            println(s"Highest available intermediate: $highestAvailableIndex")
          }
          case _ => println(s"Matched something else.")
        }
        case Failure(error) => println(s"Completed with error: $error")
      }
    }
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
