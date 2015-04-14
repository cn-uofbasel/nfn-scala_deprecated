package filterAccess.service.key

import akka.actor.ActorRef
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.KeyPersistency
import filterAccess.persistency.PermissionPersistency
import filterAccess.crypto.Encryption._
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._
import nfn.service._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class KeyChannel extends NFNService {

  /** public key (identity) */
  private var publicKey: String = "missing public key"

  /** corresponding private key */
  private var privateKey: String = "missing private key"

  /**
   * Set publicKey (public key) of this service.
   * @param   id    Public Key
   */
  // TODO
  def setPublicKey(id: String): Unit = {
    publicKey = id
  }

  /**
   * Get publicKey (public key) of this service.
   * @return  Public Key
   */
  def getPublicKey: String = publicKey

  /**
   * Set private key corresponding to public key (publicKey).
   * @param   id    Public Key
   */
  def setPrivateKey(id: String): Unit = {
    privateKey = id
  }

  /**
   * Get private key corresponding to public key (publicKey).
   * @return
   */
  def getPrivateKey: String = privateKey

  /**
   *
   * @param   content   Name of content object containing the actual data
   * @param   level     Access level
   * @param   id        User Identity (PubKey)
   * @return            Key (if allowed)
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

      // could not parse name
      case _ => None

    }

  }

  /**
   * Set a custom name for this Service
   */
  // override def ccnName: CCNName = CCNName("key")

  /**
   * Pin this service
   */
  override def pinned: Boolean = false // TODO

  /**
   * Entry point of this service.
   * @param args
   * @param ccnApi
   * @return
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
