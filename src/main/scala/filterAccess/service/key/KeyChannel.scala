package filterAccess.service.key

import filterAccess.service.Channel
import nfn.service._
import akka.actor.ActorRef

import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.KeyPersistency
import filterAccess.persistency.PermissionPersistency
import filterAccess.crypto.Encryption._
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._

import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the key channel.
 *
 */
class KeyChannel extends Channel {


  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    content   Raw data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @return             JSON Object
   */
  private def processKeyChannel(content: String, level: Int, id: String): Option[String] = {

    // Extract name of actual data
    DataNaming.getName(content) match {

      case Some(n) => {
        // Check permission
        val jsonPermission = PermissionPersistency.getPersistentPermission(n)
        checkPermission(jsonPermission.get, id, level) match {
          case true => {

            // Fetch json object with symmetric key
            KeyPersistency.getPersistentKey(n) match {
              case Some(jsonSymKey) => {
                // Extract symmetric key
                val symKey = extractLevelKey(jsonSymKey, level)
                // Encrypt symmetric key with users public key
                Some(pubEncrypt(symKey.get, id))
              }
              case _ => {
                // Could not fetch data from persistent storage
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

      // Could not parse name
      case _ => None

    }

  }

  /** Set a custom name for this Service */
  // override def ccnName: CCNName = CCNName("key")

  /** Pin this service */
  override def pinned: Boolean = false // TODO

  /**
   * Entry point of this service.
   *
   * @param    args     Function arguments
   * @param    ccnApi   Akka Actor
   * @return            Functions result
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(content), NFNIntValue(level), NFNStringValue(id)) => {
        processKeyChannel(new String(content), level, new String(id)) match {
          case Some(key) => {
            // TODO
            // If the first character is a number just parts of the string is returned
            // -> CCNLite/NFN-Scala Bug?
            NFNStringValue(key)
          }
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, data not found, invalid access level")
        }
      }

      case _ => {
        throw new NFNServiceArgumentException(s"KeyChannel Service: Argument mismatch.")
      }
    }

  }

}
