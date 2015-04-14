package filterAccess.tools

import akka.pattern._
import akka.actor.ActorRef
import akka.util.Timeout
import ccn.packet.{CCNName, Interest, Content}
import nfn.NFNApi

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Helper functions for networking tasks.
 *
 */

object Networking {

  /**
   * Try to fetch content object by given interest.
   *
   * @param    interest Interest to send out
   * @param    ccnApi   Actor Reference
   * @param    time     Timeout
   * @return
   */
  def fetchContent(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    // form interest for permission data

    // try to fetch permission data and return if successful
    val futServiceContent: Future[Content] = loadFromCacheOrNetwork(interest)
    Await.result(futServiceContent, time) match {
      case c: Content => Some(c)
      case _ => None
    }
  }

  /**
   * Try to fetch content object by name.
   *
   * @param    name     Name
   * @param    ccnApi   Actor
   * @param    time     Timeout
   * @return
   */
  def fetchContent(name: String, ccnApi: ActorRef, time: Duration): Option[Content] = {
    val i = Interest(CCNName(name.split("/").tail: _*))
    fetchContent(i, ccnApi, time)
  }

}
