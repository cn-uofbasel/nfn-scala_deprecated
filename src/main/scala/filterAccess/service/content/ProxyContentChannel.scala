package filterAccess.service.content

import akka.actor.ActorRef
import scala.concurrent.duration._

import filterAccess.tools.Exceptions.noReturnException
import filterAccess.tools.InterestBuilder.buildContentChannelInterest
import filterAccess.tools.Networking.fetchContent


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a content channel proxy. This kind of service hands an interest over to more specialized
 * service. In this case, the function parameter level:Int determines to which specific service an interest is mapped.
 * A proxy might also implement load balancing.
 *
 */
class ProxyContentChannel extends ContentChannel {

  override def processContentChannel(name: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // build interest
    val interest = level match {
      case l if l<1 => {
        // negative access level
        throw new noReturnException("No return. Invalid access level. You asked for access level " + level + ".")
      }
      case 1 => {
        // raw data
        buildContentChannelInterest(name, 1)
      }
      case l if l>1 => {
        // processed data
        buildContentChannelInterest(name, l)
      }
    }

    // fetch and return
    fetchContent(interest, ccnApi, 5 seconds) match {
      case Some(content) => Some(new String(content.data))
      case None => ???
    }

  }

}