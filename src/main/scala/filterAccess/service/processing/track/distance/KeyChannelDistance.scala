package filterAccess.service.processing.track.distance

import akka.actor.ActorRef
import filterAccess.json.KeyChannelParser._
import filterAccess.tools.{Networking, DataNaming}
import nfn.service._

import filterAccess.tools.Exceptions.noReturnException
import Networking._
import filterAccess.crypto.Encryption.pubEncrypt

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Key channel implementation of the "distance" processing service.
 *
 */
class KeyChannelDistance extends Distance {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN   Extended relative data name
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  def processKeyChannel(extRDN: String, level:Int, pubKey: String, ccnApi: ActorRef): Option[String] = {

    // determine key to which access level should be used to synthesize new key (highest possible)
    // 0 if permission data, 1 else
    val contentLevel = if (level == 0) 0 else 1
    // TODO -- contentLevel should be 2 in the latter case, but this causes timeouts! (See ContentChannelDistance.scala)
    // fetch necessary content
    fetchAndDecrypt(extRDN, (true, false, true), contentLevel, contentLevel, ccnApi, getPublicKey, getPrivateKey) match {
      case (Some(permission), _, Some(key)) => {
        // check permission
        checkPermission(permission, pubKey, contentLevel) match {
          case true => {
            // synthesize symmetric key
            val symKey = keySyntesizer(key, "salt123foo!", "pepper456bar?")
            // encrypt synthesized symmetric key with public key
            Some(pubEncrypt(symKey,pubKey))
          }
          case false => {
            // Permission denied
            None
          }
        }
      }
      case _ => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN, data not available..")
    }
  }


  /** Pin this service */
  override def pinned: Boolean = false

  /**
   * Entry point of this service.
   *
   * @param    args     Function arguments
   * @param    ccnApi   Akka Actor
   * @return            Functions result
   */
  def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(extRDN), NFNIntValue(level), NFNStringValue(pubKey)) => {
        processKeyChannel(new String(extRDN), level, new String(pubKey), ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}