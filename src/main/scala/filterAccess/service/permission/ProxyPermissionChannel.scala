package filterAccess.service.permission

import akka.actor.ActorRef
import filterAccess.tools.InterestBuilder._
import scala.concurrent.duration._

import filterAccess.tools.Networking.fetchContent

// enable postfix operator seconds
import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a permission channel proxy. This kind of service hands an interest over to more
 * specialized service. In this case, a request is just redirected to another service. A proxy might also
 * implement load balancing.
 */
class ProxyPermissionChannel extends PermissionChannel {

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn       Relative data name
   * @return             JSON Object
   */
  override def processPermissionChannel(rdn: String, ccnApi: ActorRef): Option[String] = {

    // build interest
    val interest = buildPermissionChannelInterest(rdn)

    // fetch and return
    fetchContent(interest, ccnApi, 5 seconds) match {
      case Some(content) => Some(new String(content.data))
      case None => ???
    }

  }

}
