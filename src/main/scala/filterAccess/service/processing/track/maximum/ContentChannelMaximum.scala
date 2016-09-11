package filterAccess.service.processing.track.maximum

import akka.actor.ActorRef
import ccn.packet.CCNName
import filterAccess.service.processing.track.distance.DistanceAPI.fetchAndDecrypt
import nfn.service._

import scala.concurrent.duration._
import filterAccess.tools.Exceptions.noReturnException
import filterAccess.crypto.Encryption._
import filterAccess.crypto.Helpers.{computeHash, symKeyGenerator}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Content channel implementation of the "maximum" processing service.
 *
 */
class ContentChannelMaximum extends Maximum {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN1     Extended relative data name
   * @param    extRDN2     Extended relative data name
   * @param    ccnApi      Akka Actor
   * @return               JSON Object
   */
  def processContentChannel(extRDN1: String, extRDN2: String, ccnApi: ActorRef): Option[String] = {

    // determine access level of needed data (lowest possible)
    val contentLevel = 1
    // fetch content (extRDN1)
    fetchAndDecrypt(extRDN1, (false, true, true), contentLevel, contentLevel, ccnApi, getPublicKey, getPrivateKey) match {
      case (_, Some(content1), Some(key1)) => {
        // fetch content (extRDN2)
        fetchAndDecrypt(extRDN2, (false, true, true), contentLevel, contentLevel, ccnApi, getPublicKey, getPrivateKey) match {
          case (_, Some(content2), Some(key2)) => {
            // compare distance
            val responseData =
              if (content1.toInt > content2.toInt)
                "The former track is longer."
              else if (content1.toInt < content2.toInt)
                "The latter track is longer."
              else
                "Both tracks have the same length."
            // synthesize symmetric key
            val responseSymKey = keySyntesizer(key1, key2, "456hello!", "123world?")
            // encrypt synthesized symmetric key with public key
            Some(symEncrypt(responseData, responseSymKey))
          }
          case _ => {
            // could not fetch data for extRDN2
            throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN, data not available..")
          }
        }
      }
      case _ => {
        // could not fetch data for extRDN1
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
  def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(extRDN1), NFNStringValue(extRDN2)) => {
        processContentChannel(extRDN1, extRDN2, ccnApi) match {
          case Some(dist) => NFNStringValue(dist)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}
