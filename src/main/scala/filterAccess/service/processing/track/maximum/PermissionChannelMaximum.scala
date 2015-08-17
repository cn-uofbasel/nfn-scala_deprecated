package filterAccess.service.processing.track.maximum

import akka.actor.ActorRef
import filterAccess.json.UserLevel
import filterAccess.tools.DataNaming
import nfn.service._

import filterAccess.crypto.Encryption._
import filterAccess.tools.Exceptions.noReturnException
import filterAccess.service.processing.track.distance.DistanceAPI._
import filterAccess.json.PermissionChannelBuilder.conjunctiveMerge

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Permission channel implementation of the "maximum" processing service.
 *
 */
class PermissionChannelMaximum extends Maximum {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN1   Extended relative data name
   * @param    extRDN2   Extended relative data name
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  def processPermissionChannel(extRDN1: String, extRDN2: String, ccnApi: ActorRef): Option[String] = {

    // fetch content (extRDN1)
    fetchAndDecrypt(extRDN1, (true, false, true), 0, 0, ccnApi, getPublicKey, getPrivateKey) match {
      case (Some(permission1), _, Some(key1)) => {
        // fetch content (extRDN2)
        fetchAndDecrypt(extRDN2, (true, false, true), 0, 0, ccnApi, getPublicKey, getPrivateKey) match {
          case (Some(permission2), _, Some(key2)) => {
            // merge permission data
            val responseData = conjunctiveMerge("maximum(" + extRDN1 + "," + extRDN2 + ")", permission1, permission2)
            // TODO -- eliminate duplicates!
            // synthesize symmetric key
            val responseSymKey = keySyntesizer(key1, key2, "456hello!", "123world?")
            // encrypt response with synthesized symmetric key and return
            Some(symEncrypt(responseData, responseSymKey))          }
          case _ => {
            // could not fetch content (extRDN2)
            throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN, data not available..")
          }
        }
      }
      case _ => {
        // could not fetch content (extRDN1)
        throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN, data not available..")
      }
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
      case Seq(NFNStringValue(extRDN1), NFNStringValue(extRDN2)) => {
        processPermissionChannel(new String(extRDN1), new String(extRDN2), ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN..")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}