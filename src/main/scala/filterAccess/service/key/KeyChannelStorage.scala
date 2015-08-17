package filterAccess.service.key

import akka.actor.ActorRef
import filterAccess.crypto.Encryption._
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{KeyPersistency, PermissionPersistency}
import filterAccess.tools.DataNaming

import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the key channel.
 *
 */
class KeyChannelStorage extends KeyChannel {


  /**
   *
   * @param    rdn       Relative data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  override def processKeyChannel(rdn: String, level: Int, id: String, ccnApi: ActorRef): Option[String] = {


    // Check permission
    val jsonPermission = PermissionPersistency.getPersistentPermission(rdn)
    checkPermission(jsonPermission.get, id, level) match {
      case true => {

        // Fetch json object with symmetric key
        KeyPersistency.getPersistentKey(rdn) match {
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

}
