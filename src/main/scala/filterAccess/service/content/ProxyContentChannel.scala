package filterAccess.service.content

import akka.actor.ActorRef
import filterAccess.tools.ConfigReader._
import filterAccess.tools.Networking
import scala.concurrent.duration._

import filterAccess.tools.Exceptions.noReturnException
import filterAccess.tools.InterestBuilder.buildDirectContentChannelInterest
import nfn.tools.Networking.fetchContent

// enable postfix operator seconds
import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a content channel proxy. This kind of service hands an interest over to more specialized
 * service. In this case, the function parameter level:Int determines to which specific service an interest is mapped.
 * A proxy might also implement load balancing.
 *
 */
class ProxyContentChannel extends ContentChannel {

  /** Config: Prefix or storage service */
  val storagePrefix =  getValueOrDefault("dpu.prefix.content.storage", "/serviceprovider/health/storage")

  /** Config: Prefix or processing service */
  val filteringPrefix =  getValueOrDefault("dpu.prefix.content.filtering", "/serviceprovider/health/filtering")


  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn      Relative data name
   * @param    level    Access level
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  override def processContentChannel(rdn: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // build interest
    val interest = level match {
      case l if l<1 => {
        // negative access level
        throw new noReturnException("No return. Invalid access level. You asked for access level " + level + ".")
      }
      case 1 => {
        // unprocessed data
        buildDirectContentChannelInterest(rdn, storagePrefix, 1)
      }
      case l if l>1 => {
        // filtered data
        buildDirectContentChannelInterest(rdn, filteringPrefix, l)
      }
    }
    
    // fetch and return
    fetchContent(interest, ccnApi, 5 seconds) match {
      case Some(content) => Some(new String(content.data))
      case None => ???
    }

  }

}