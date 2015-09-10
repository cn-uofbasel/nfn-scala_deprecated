package filterAccess.service.content

import akka.actor.ActorRef
import ccn.packet._

import filterAccess.crypto.Encryption._

import filterAccess.json.ContentChannelParser
import filterAccess.json.ContentChannelBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

import filterAccess.tools.Networking._
import filterAccess.tools.Exceptions._
import filterAccess.tools.DataNaming._
import filterAccess.tools.ConfigReader._


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the content channel (filtering on DSU).
 *
 */
class ContentChannelFiltering extends ContentChannel {

  /** Config: Prefix of this service (respectively its proxy).
   * (Needed to reach ChontentChannelProcessing to fetch unfiltered data)
   */
  val ownPrefix = getValueOrDefault("dcu.proxy.prefix", "/own/machine")

  /**
   * Filtering function for type:track on first filtering level (access level 2).
   *
   * @param   data   Unfiltered data
   * @return         Filtered data
   */
  def filterTrack2(data: String): Option[String] = {
    // extract trace and name
    val trace = ContentChannelParser.getTrace(data).get
    val name = ContentChannelParser.getName(data).get

    // actual filtering
    val offset = trace(0)
    val filtered_trace = (for (point <- trace) yield point - offset)

    // rebuild json
    Some {
      ContentChannelBuilder.buildTrack(filtered_trace, name)
    }
    // TODO more sophisticated error handling
  }

  /**
   * Filtering function for type:track on second filtering level (access level 3).
   *
   * @param   data   Unfiltered data
   * @return         Filtered data
   */
  def filterTrack3(data: String): Option[String] = {
    // extract trace and name
    val trace = ContentChannelParser.getTrace(data).get
    val name = ContentChannelParser.getName(data).get

    // actual filtering
    val filtered_trace = ??? // TODO

    // rebuild json
    Some {
      ContentChannelBuilder.buildTrack(filtered_trace, name)
    }
    // TODO more sophisticated error handling
  }


  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn      Relative data name
   * @param    level    Access level
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  override def processContentChannel(rdn: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // check if this call should be satisfied by a processing service/unit
    if (level < 2)
      throw new noReturnException(s"No return. This is a filtering unit so only filtered data is delivered. You asked for access level ${level}.")

    // fetch all needed data (content, permission, symmetric key)
    fetchAndDecrypt(rdn + "@" + ownPrefix, (false, true, true), level, 1, ccnApi, getPublicKey, getPrivateKey) match {
      case (_, Some(content), Some(symKey)) => {

        // handle data type
        getType(rdn) match {

          case Some("type:track") => {
            // call filtering function
            level match {
              case 2 => Some( symEncrypt(filterTrack2(content).get, symKey) )
              case 3 => Some( symEncrypt(filterTrack3(content).get, symKey) )
              case _ => throw new noReturnException(s"No return. Did not find a filter for access level ${level}.")
            }
          }

          case _ => throw new noReturnException(s"No return. Did not find filters for this data type.")

        }

      }

      case _ => None

    }

  }

}
