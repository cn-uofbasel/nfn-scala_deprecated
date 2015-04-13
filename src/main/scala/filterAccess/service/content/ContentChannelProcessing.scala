package filterAccess.service.content

import akka.actor.ActorRef
import ccn.packet._

import filterAccess.crypto.Decryption._
import filterAccess.crypto.Encryption._

import filterAccess.json.ContentChannelParser
import filterAccess.json.ContentChannelBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

import filterAccess.tools.InterestBuilder._
import filterAccess.tools.Networking.fetchContent
import filterAccess.tools.Exceptions._
import filterAccess.tools.DataNaming._


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class ContentChannelProcessing extends ContentChannel {

  /**
   *
   * @param data
   * @return
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
   *
   * @param data
   * @return
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
   * Fetch and decrypt unprocessed (access level 0 and 1) content channel, permission/access channel and key channel data by name.
   *
   * @param    name      Name
   * @param    level     Access Level
   * @param    selector  Select which data should be returned (permission, content, symmetric key)
   * @param    ccnApi    Actor Reference
   * @param    timeout   Timeout (standard value: 5 seconds)
   * @return
   */

  def fetchAndDecrypt(name: String, level: Int, selector: (Boolean, Boolean, Boolean), ccnApi: ActorRef, timeout: FiniteDuration = 5 seconds): (Option[String], Option[String], Option[String]) = {

    /**
     * Perform actual fetching and decryption with asymmetric encryption (key channel)
     *
     * @param    dataInterest     Interest to fetch certain data
     * @param    timeout          Timeout
     * @return
     */
    def performFetchAndDecryptAsymmetric(dataInterest: Interest, timeout: FiniteDuration = timeout): Option[String] = {
      //fetch data (encrypted with public key)
      fetchContent(dataInterest, ccnApi, timeout) match {
        // decryption
        case Some(data: Content) => Some(privateDecrypt(new String(data.data), getPrivateKey))
        case _ => None
      }
    }

    /**
     * Perform actual fetching and decryption with symmetric encryption (access/permission or content channel)
     *
     * @param    dataInterest     Interest to fetch certain data
     * @param    keyInterest      Interest to fetch corresponding key
     * @param    timeout          Timeout
     * @return
     */
    def performFetchAndDecryptSymmetric(dataInterest: Interest, keyInterest: Interest, timeout: FiniteDuration = timeout): Option[String] = {
      // fetch data (encrypted with public key)
      fetchContent(dataInterest, ccnApi, timeout) match {
        case Some(data: Content) => {
          // fetch corresponding key (encrypted with public key)
          fetchContent(keyInterest, ccnApi, timeout) match {
            case Some(key: Content) => {
              // decryption
              val symKey = privateDecrypt(new String(key.data), getPrivateKey)
              Some(symDecrypt(new String(data.data), symKey))
            }
            case _ => None
          }
        }
        case _ => None
      }
    }

    // fetch permission channel if selected
    val permissionResult = selector._1 match {
      case true => {
        // build interests for permission channel
        val permissionInterest = buildPermissionChannelInterest(name)
        val permissionKeyInterest = buildKeyChannelInterest(name, 0, getPublicKey)
        //fetch
        performFetchAndDecryptSymmetric(permissionInterest, permissionKeyInterest)
      }
      case false => None
    }

    // fetch content channel if selected
    val contentResult = selector._2 match {
      case true => {
        // build interests for content channel
        val contentInterest = buildContentChannelInterest(name, 1)
        val contentKeyInterest = buildKeyChannelInterest(name, 1, getPublicKey)
        //fetch
        performFetchAndDecryptSymmetric(contentInterest, contentKeyInterest)
      }
      case false => None
    }

    // fetch key channel if selected
    val keyResult = selector._3 match {
      case true => {
        // build interest for key channel
        val keyInterest = buildKeyChannelInterest(name, level, getPublicKey)
        //fetch
        performFetchAndDecryptAsymmetric(keyInterest)
      }
      case false => None
    }

    // return
    (permissionResult, contentResult, keyResult)

  }


  /**
   *
   * @param name
   * @param level
   * @return
   */
  override def processContentChannel(name: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // check if this call should be satisfied by a processing service/unit
    if (level < 2)
      throw new noReturnException(s"No return. This is a processing unit so only processed data is delivered. You asked for access level ${level}.")

    // fetch all needed data (content, permission, symmetric key)
    fetchAndDecrypt(name, level, (false, true, true), ccnApi) match {
      case (None, Some(content), Some(symKey)) => {

        // handle data type
        getType(name) match {

          case Some("type:track") => {
            // call filtering function
            level match {
              case 2 => Some( symEncrypt(filterTrack2(content).get, symKey) )
              case 3 => Some( symEncrypt(filterTrack3(content).get, symKey) )
              case _ => throw new noReturnException(s"No return. Did not a filter for access level ${level}.")
            }
          }

          case _ => throw new noReturnException(s"No return. Did not find filters for this data type.")

        }

      }

      case _ => None

    }

  }

}
