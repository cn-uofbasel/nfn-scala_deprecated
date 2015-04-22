package filterAccess.service.access

import akka.actor.ActorRef
import filterAccess.tools.InterestBuilder._
import scala.concurrent.duration._

import filterAccess.tools.Networking.fetchContent

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a access channel proxy. This kind of service hands an interest over to more specialized
 * service. In this case, a request is just redirected to another service. A proxy might also implement load balancing.
 */
class ProxyAccessChannel extends AccessChannel {

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    name      Raw data name
   * @return             JSON Object
   */
  override def processAccessChannel(name: String, ccnApi: ActorRef): Option[String] = {

    // build interest
    val interest = buildPermissionChannelInterest(name)

    // fetch and return
    fetchContent(interest, ccnApi, 5 seconds) match {
      case Some(content) => Some(new String(content.data))
      case None => ???
    }

  }

}
