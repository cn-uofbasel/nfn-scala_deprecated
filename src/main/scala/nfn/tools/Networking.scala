package nfn.tools

import java.util.concurrent.TimeoutException

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet.{CCNName, Content, Interest}
import akka.actor.ActorRef
import akka.actor.Status._
import akka.util.Timeout
import ccn.packet.{CCNName, Content, Interest}
import nfn.NFNApi

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

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
}
