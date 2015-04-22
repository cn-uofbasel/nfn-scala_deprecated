package filterAccess.service.key

import akka.actor.ActorRef
import filterAccess.tools.InterestBuilder._
import scala.concurrent.duration._

import filterAccess.tools.Networking.fetchContent

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a key channel proxy. This kind of service hands an interest over to more specialized
 * service. In this case, a request is just redirected to another service. A proxy might also implement load balancing.
 *
 */
class ProxyKeyChannel extends KeyChannel {


  /**
   *
   * @param    name      Raw data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  override def processKeyChannel(name: String, level: Int, id: String, ccnApi: ActorRef): Option[String] = {

    // build interest
    val interest = buildKeyChannelInterest(name, level, id)

    // fetch and return
    fetchContent(interest, ccnApi, 5 seconds) match {
      case Some(content) => Some(new String(content.data))
      case None => ???
    }

  }

}
