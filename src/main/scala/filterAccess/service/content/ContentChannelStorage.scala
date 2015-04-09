package filterAccess.service.content

import akka.actor.ActorRef
import filterAccess.encoding.Encryption._
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{KeyPersistency, ContentPersistency}
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions.noReturnException

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class ContentChannelStorage extends ContentChannel {

  /**
   *
   * @param name
   * @return
   */
  override def processContentChannel(name: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // check if this call should be satisfied by a storage service/unit
    if (level != 1)
      throw new noReturnException("No return. This is a storage unit so only unprocessed data is delivered. You asked for access level " + level + ".")

    // Extract name of actual data
    DataNaming.getName(name) match {

      case Some(n) => {

        // Fetch json object with symmetric key
        ContentPersistency.getPersistentContent(n) match {
          case Some(content) => {
            // Fetch JSON with symmetric keys
            KeyPersistency.getPersistentKey(n) match {
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

      // could not parse name
      case _ => None

    }

  }

}
