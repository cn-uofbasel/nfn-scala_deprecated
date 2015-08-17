package filterAccess.service.content

import akka.actor.ActorRef
import filterAccess.crypto.Encryption._
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{KeyPersistency, ContentPersistency}
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions.noReturnException

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * This class is used to set up a service for the content channel (storage on DSU).
 *
 */
class ContentChannelStorage extends ContentChannel {

  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn      Relative data name
   * @param    level    Access level
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  override def processContentChannel(rdn: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // check if this call should be satisfied by a storage service/unit
    if (level != 1)
      throw new noReturnException("No return. This is a storage unit so only unprocessed data is delivered. You asked for access level " + level + ".")

    // Fetch json object with symmetric key
    ContentPersistency.getPersistentContent(rdn) match {
      case Some(content) => {
        // Fetch JSON with symmetric keys
        KeyPersistency.getPersistentKey(rdn) match {
          case Some(jsonSymKey) => {
            // Extract needed symmetric key
            val symKey = extractLevelKey(jsonSymKey, level)
            // Encrypt with symmetric key
            Some(symEncrypt(content, symKey.get))
          }
          case _ => {
            // Could not fetch data from persistent storage
            None
          }
        }
      }

      case _ => {
        // Could not fetch data from persistent storage
        None
      }
    }
  }

}
