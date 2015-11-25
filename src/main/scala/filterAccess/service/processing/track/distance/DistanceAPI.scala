package filterAccess.service.processing.track.distance

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest}
import filterAccess.crypto.Decryption._

import filterAccess.tools.{Networking, DataNaming}
import nfn.tools.Networking.fetchContent

import scala.concurrent.duration._
import scala.language.postfixOps

import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Client library to generate interests to invoke the processing service "distance" and to request certain information
 * just by calling a function.
 *
 */
object DistanceAPI {


  //--------------------------------------------------------------------------------------------------------------


  /*
  *
  *   NOTE: LEAVE AWAY THE "/" AT THE BEGINNING! WHY?
  *
  *
  *   OBSERVATION:
  *   The following line of code...
  *     println(CCNName("/this/is/the/name").name)
  *   ..prints out:
  *     //this/is/the/name
  *
  *    WORK-AROUND:
  *      CCNName( "/this/is/the/name".substring(1) )
  *
  *
  *   THIS LOOKS LIKE A BUG IN NFN-SCALA.
  *
  */

  //--------------------------------------------------------------------------------------------------------------

  /*
   *
   * NOTE: The names of the services should principally be ContentChannel, KeyChannel, PermissionChannel.
   * Unfortunately, the current nfn-scala implementation does not support custom service names, but generates
   * its own names due to the package structure in scala.
   * To overcome this situation, we hard code the names generated names in this file.
   *
   */

  //--------------------------------------------------------------------------------------------------------------

  /** Config: Prefix */
  val servicePrefix = "/processing/provider"

  /** Config: Name of service for content channel */
  val contentChannelServiceName = "filterAccess_service_processing_track_distance_ContentChannelDistance"

  /** Config: Name of service for key channel */
  val keyChannelServiceName = "filterAccess_service_processing_track_distance_KeyChannelDistance"

  /** Config: Name of service for permission channel */
  val permissionChannelServiceName = "filterAccess_service_processing_track_distance_PermissionChannelDistance"


  //--------------------------------------------------------------------------------------------------------------

  /**
   * Content channel interest builder for the "distance" processing service.
   *
   * @param    rdn             Relative data name of the asked content
   * @param    prefix          Prefix of the storage location
   * @param    contentLevel    Access Level
   * @return                   Generated Interest
   */
  def buildContentChannelInterest(rdn: String, prefix: String, contentLevel: Int): Interest = {
    ???
  }

  /**
   * Content channel interest builder for the "distance" processing service.
   *
   * @param    extRDN          Extended relative data name of the content which should be processed
   * @param    contentLevel    Access Level
   * @return                   Generated Interest
   */
  def buildContentChannelInterest(extRDN: String, contentLevel: Int): Interest =
    CCNName(servicePrefix.substring(1) + "/" + contentChannelServiceName) call(extRDN, contentLevel)


  /**
   * Key channel interest builder for the "distance" processing service.
   *
   * @param    rdn             Relative data name of the asked content
   * @param    prefix          Prefix of the storage location
   * @param    keyLevel        Access Level
   * @param    pubKey          Callers public key
   * @return                   Generated Interest
   */
  def buildKeyChannelInterest(rdn: String, prefix: String, keyLevel: Int, pubKey: String): Interest = {
    ???
  }

  /**
   * Key channel interest builder for the "distance" processing service.
   *
   * @param    extRDN          Extended relative data name of the content which should be processed
   * @param    keyLevel        Access Level
   * @param    pubKey          Callers public key
   * @return                   Generated Interest
   */
  def buildKeyChannelInterest(extRDN: String, keyLevel: Int, pubKey: String): Interest =
    CCNName(servicePrefix.substring(1) + "/" + keyChannelServiceName) call(extRDN, keyLevel, pubKey)

  /**
   * Permission channel interest builder for the "distance" processing service.
   *
   * @param    rdn             Relative data name of the asked content
   * @param    prefix          Prefix of the storage location
   * @return                   Generated Interest
   */
  def buildPermissionChannelInterest(rdn: String, prefix: String): Interest = {
    ???
  }

  /**
   * Permission channel interest builder for the "distance" processing service.
   *
   * @param    extRDN          Extended relative data name of the content which should be processed
   * @return                   Generated Interest
   */
  def buildPermissionChannelInterest(extRDN: String): Interest =
    CCNName(servicePrefix.substring(1) + "/" + permissionChannelServiceName) call(extRDN)



  /**
   * Fetch and decrypt content channel, permission channel and key channel data by rdn and access level for the "distance" service.
   *
   * @param    extRDN          Extended relative data name (<rdn>@<location-prefix>)
   * @param    selector        Select which data should be returned (permission, content, symmetric key)
   * @param    keyLevel        Key to which access level?
   * @param    contentLevel    Content to which access level?
   * @param    ccnApi          Actor Reference
   * @param    publicKey       Callers public key
   * @param    privateKey      Callers private key
   * @param    timeout         Timeout (standard value: 5 seconds)
   * @return                   (permission, content, key)
   */
  def fetchAndDecrypt(extRDN: String, selector: (Boolean, Boolean, Boolean), keyLevel: Int, contentLevel: Int, ccnApi: ActorRef, publicKey: String, privateKey: String, timeout: FiniteDuration = 5 seconds): (Option[String], Option[String], Option[String]) = {

    /**
     * Perform actual fetching and decryption with asymmetric encryption (key channel)
     *
     * @param    dataInterest     Interest to fetch certain data
     * @param    timeout          Timeout
     * @return                    Content
     */
    def performFetchAndDecryptAsymmetric(dataInterest: Interest, timeout: FiniteDuration = timeout): Option[String] = {
      //fetch data (encrypted with public key)
      fetchContent(dataInterest, ccnApi, timeout) match {
        // decryption
        case Some(data: Content) => Some(privateDecrypt(new String(data.data), privateKey))
        case _ => None
      }
    }

    /**
     * Perform actual fetching and decryption with symmetric encryption (permission or content channel)
     *
     * @param    dataInterest     Interest to fetch certain data
     * @param    keyInterest      Interest to fetch corresponding key
     * @param    timeout          Timeout
     * @return                    Content
     */
    def performFetchAndDecryptSymmetric(dataInterest: Interest, keyInterest: Interest, timeout: FiniteDuration = timeout): Option[String] = {
      // fetch data (encrypted with public key)
      fetchContent(dataInterest, ccnApi, timeout) match {
        case Some(data: Content) => {
          // fetch corresponding key (encrypted with public key)
          fetchContent(keyInterest, ccnApi, timeout) match {
            case Some(key: Content) => {
              // decryption
              val symKey = privateDecrypt(new String(key.data), privateKey)
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
        val permissionInterest = buildPermissionChannelInterest(extRDN)
        val permissionKeyInterest = buildKeyChannelInterest(extRDN, 0, publicKey)
        //fetch
        performFetchAndDecryptSymmetric(permissionInterest, permissionKeyInterest)
      }
      case false => None
    }

    // fetch content channel if selected
    val contentResult = selector._2 match {
      case true => {
        // build interests for content channel
        val contentInterest = buildContentChannelInterest(extRDN, contentLevel)
        val contentKeyInterest = buildKeyChannelInterest(extRDN, contentLevel, publicKey)
        //fetch
        performFetchAndDecryptSymmetric(contentInterest, contentKeyInterest)
      }
      case false => None
    }

    // fetch key channel if selected
    val keyResult = selector._3 match {
      case true => {
        // build interest for key channel
        val keyInterest = buildKeyChannelInterest(extRDN, keyLevel, publicKey)
        //fetch
        performFetchAndDecryptAsymmetric(keyInterest)
      }
      case false => None
    }

    // return
    (permissionResult, contentResult, keyResult)

  }

}
