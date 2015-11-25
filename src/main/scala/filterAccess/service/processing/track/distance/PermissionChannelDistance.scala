package filterAccess.service.processing.track.distance

import akka.actor.ActorRef
import filterAccess.json.UserLevel
import filterAccess.tools.{Networking, DataNaming}
import nfn.service._

import filterAccess.crypto.Encryption._
import filterAccess.tools.Exceptions.noReturnException
import Networking._
import filterAccess.json.PermissionChannelBuilder.{manipulateLevel,minimizePermissions}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Permission channel implementation of the "distance" processing service.
 *
 */
class PermissionChannelDistance extends Distance {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    extRDN    Extended relative data name
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  def processPermissionChannel(extRDN: String, ccnApi: ActorRef): Option[String] = {

    // fetch necessary content
    fetchAndDecrypt(extRDN, (true, false, true), 0, 0, ccnApi, getPublicKey, getPrivateKey) match {
      case (Some(permission), _, Some(key)) => {
        // minimize permission data (delete all entries with to high access levels)
        val minData = minimizePermissions(permission, 2)
        // adjust access level of minimized entries (0 to 0, greater/equal 1 to 1)
        val responseData = manipulateLevel(minData, "distance(" + extRDN +")", ul => (ul.level >=1), ul => UserLevel(ul.name,1))
        // synthesize symmetric key
        val responseSymKey = keySyntesizer(key, "salt123foo!", "pepper456bar?")
        // encrypt response with synthesized symmetric key and return
        Some(symEncrypt(responseData, responseSymKey))
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
      case Seq(NFNStringValue(extRDN)) => {
        processPermissionChannel(new String(extRDN), ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid RDN..")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}