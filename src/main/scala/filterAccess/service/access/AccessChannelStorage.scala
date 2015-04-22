package filterAccess.service.access

import akka.actor.ActorRef
import filterAccess.crypto.Encryption._
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{KeyPersistency, PermissionPersistency}
import filterAccess.service.Channel
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._
import nfn.service._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the access channel.
 *
 */
class AccessChannelStorage extends AccessChannel {

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    name      Raw data name
   * @return             JSON Object
   */
  override def processAccessChannel(name: String, ccnApi: ActorRef): Option[String] = {

    // Extract name of actual data
    DataNaming.getName(name) match {

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

}
