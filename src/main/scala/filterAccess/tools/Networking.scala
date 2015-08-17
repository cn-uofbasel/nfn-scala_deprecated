package filterAccess.tools

import akka.pattern._
import akka.actor.ActorRef
import akka.util.Timeout
import ccn.packet.{CCNName, Interest, Content}
import filterAccess.crypto.Decryption._
import filterAccess.tools.InterestBuilder._
import nfn.NFNApi

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

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
      case _ => None
    }
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

  /**
   * Fetch and decrypt content channel, permission channel and key channel data by rdn and access level.
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

    // split extended relative data name into rdn and prefix
    val rdn = DataNaming.getRDN(extRDN).get
    val prefix = DataNaming.getPrefix(extRDN).get

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
        val permissionInterest = buildIndirectPermissionChannelInterest(rdn, prefix)
        val permissionKeyInterest = buildIndirectKeyChannelInterest(rdn, prefix, 0, publicKey)
        //fetch
        performFetchAndDecryptSymmetric(permissionInterest, permissionKeyInterest)
      }
      case false => None
    }

    // fetch content channel if selected
    val contentResult = selector._2 match {
      case true => {
        // build interests for content channel
        val contentInterest = buildIndirectContentChannelInterest(rdn, prefix, contentLevel)
        val contentKeyInterest = buildIndirectKeyChannelInterest(rdn, prefix, contentLevel, publicKey)
        //fetch
        performFetchAndDecryptSymmetric(contentInterest, contentKeyInterest)
      }
      case false => None
    }

    // fetch key channel if selected
    val keyResult = selector._3 match {
      case true => {
        // build interest for key channel
        val keyInterest = buildIndirectKeyChannelInterest(rdn, prefix, keyLevel, publicKey)
        //fetch
        performFetchAndDecryptAsymmetric(keyInterest)
      }
      case false => None
    }

    // return
    (permissionResult, contentResult, keyResult)

  }

}
