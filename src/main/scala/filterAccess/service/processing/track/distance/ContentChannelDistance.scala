package filterAccess.service.processing.track.distance

import akka.actor.ActorRef
import filterAccess.tools.DataNaming
import nfn.service._

import scala.concurrent.duration._

import filterAccess.tools.Exceptions.noReturnException
import filterAccess.tools.Networking._
import filterAccess.json.ContentChannelParser.getTrack
import filterAccess.json._
import filterAccess.crypto.Encryption.symEncrypt
import filterAccess.crypto.Helpers.{symKeyGenerator,computeHash,stringToByte,byteToString}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Content channel implementation of the "distance" processing service.
 *
 */
class ContentChannelDistance extends Distance {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN   Extended relative data name
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  def processContentChannel(extRDN: String, contentLevel: Int,  ccnApi: ActorRef): Option[String] = {

    // content level must be 1
    assert(contentLevel == 1)

    // determine access level of needed data (lowest possible)
    val inputLevel = 1 // -- TODO: inputLevel should be 2, but this causes timeouts!

    // fetch necessary content
    fetchAndDecrypt(extRDN, (false, true, true), inputLevel, inputLevel, ccnApi, getPublicKey, getPrivateKey) match {
      // successfully retrieved necessary content
      case (_, Some(content), Some(key)) => {
        // extract trace
        getTrack(content) match {
          // successfully extracted
          case t:Some[Track] => {
            // prepare response and synthesize symmetric encryption key
            val responseData = t.get.length.toString
            val responseSymKey = keySyntesizer(key, "salt123foo!", "pepper456bar?")
            // encrypt and return
            Some(symEncrypt(responseData, responseSymKey))
          }
          // could not parse json..
          case _ => throw new noReturnException("No return. Possibly caused by: Could not parse input data. Wrong type?")
        }
      }
        // count not retrieve content
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
      case Seq(NFNStringValue(extRDN), NFNIntValue(contentLevel)) => {
        processContentChannel(extRDN, contentLevel, ccnApi) match {
          case Some(dist) => NFNStringValue(dist)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}