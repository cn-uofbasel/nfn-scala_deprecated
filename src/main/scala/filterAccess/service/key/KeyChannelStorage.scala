package filterAccess.service.key

import akka.actor.ActorRef
import filterAccess.crypto.Encryption._
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{KeyPersistency, PermissionPersistency}
import filterAccess.service.Channel
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._
import nfn.service._

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
   * @param    name      Raw data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  override def processKeyChannel(name: String, level: Int, id: String, ccnApi: ActorRef): Option[String] = {

    // Extract name of actual data
    DataNaming.getName(name) match {

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

}
