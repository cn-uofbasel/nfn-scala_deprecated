package filterAccess.service.access

import filterAccess.service.Channel
import nfn.service._
import akka.actor.ActorRef

import filterAccess.crypto.Encryption._
import filterAccess.json.{AccessChannelBuilder, UserLevel}
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{PermissionPersistency, KeyPersistency}
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the access channel.
 *
 */
class AccessChannel extends Channel {

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    content   Raw data name
   * @return             JSON Object
   */
  private def processAccessChannel(content: String): Option[String] = {

    // Extract name of actual data
    DataNaming.getName(content) match {

      case Some(n) => {

        // Fetch permission data
        PermissionPersistency.getPersistentPermission(n) match {
          case Some(jsonPermission) => {
            // Fetch json object with symmetric key
            KeyPersistency.getPersistentKey(n) match {
              case Some(jsonSymKey) => {
                // Extract symmetric key
                // Note: AccessLevel "-1" specifies key to secure permission data
                val symKey = extractLevelKey(jsonSymKey, 0)
                // Encrypt permission data with symmetric key
                Some(symEncrypt(jsonPermission, symKey.get))
              }
              case _ => {
                // Could not fetch symmetric key from persistent storage
                None
              }
            }
          }
          case None => {
            // Could not fetch permission data from persistent storage
            None
          }

        }

      }

      // Could not parse name
      case _ => None

    }

  }

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
      case Seq(NFNStringValue(content)) =>
        processAccessChannel(content) match {
          case Some(res) => NFNStringValue(res)
          case None => throw new noReturnException("No return. Possibly caused by: Data not found")
        }

      case _ =>
        throw new NFNServiceArgumentException(s"AccessChannel: Argument mismatch.")
    }

  }

}
