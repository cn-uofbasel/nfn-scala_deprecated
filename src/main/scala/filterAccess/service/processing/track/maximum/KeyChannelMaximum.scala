package filterAccess.service.processing.track.maximum

import akka.actor.ActorRef
import filterAccess.json.KeyChannelParser._
import nfn.service._

import filterAccess.tools.Exceptions.noReturnException
import filterAccess.service.processing.track.distance.DistanceAPI._
import filterAccess.crypto.Encryption.pubEncrypt

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Key channel implementation of the "maximum" processing service.
 *
 */
class KeyChannelMaximum extends Maximum {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN1   Extended relative data name
   * @param    extRDN2   Extended relative data name
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  def processKeyChannel(extRDN1: String, extRDN2: String, level: Int, pubKey: String, ccnApi: ActorRef): Option[String] = {


    // determine key to which access level should be used to synthesize new key (highest possible)
    // here: 0 if permission data, 1 else
    val contentLevel = if (level == 0) 0 else 1
    // fetch content (extRDN1)
    fetchAndDecrypt(extRDN1, (true, false, true), contentLevel, contentLevel, ccnApi, getPublicKey, getPrivateKey) match {
      case (Some(permission1), _, Some(key1)) => {
        // check permission
        checkPermission(permission1, pubKey, contentLevel) match {
          case true => {
            // fetch content (extRDN2)
            fetchAndDecrypt(extRDN2, (true, false, true), contentLevel, contentLevel, ccnApi, getPublicKey, getPrivateKey) match {
              case (Some(permission2), _, Some(key2)) => {
                //check permission
                checkPermission(permission2, pubKey, contentLevel) match {
                  case true => {
                    // synthesize symmetric key
                    val symKey = keySyntesizer(key1, key2, "456hello!", "123world?")
                    // encrypt synthesized symmetric key with public key
                    Some(pubEncrypt(symKey, pubKey))
                  }
                  case false => {
                    // Permission denied
                    None
                  }
                }
              }
              case _ => {
                // Permission denied
                None
              }
            }
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
      case Seq(NFNStringValue(extRDN1), NFNStringValue(extRDN2), NFNIntValue(level), NFNStringValue(pubKey)) => {
        processKeyChannel(new String(extRDN1), new String(extRDN2), level, new String(pubKey), ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}