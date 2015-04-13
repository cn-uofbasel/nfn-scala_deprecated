package filterAccess.service.access

import akka.actor.ActorRef
import filterAccess.crypto.Encryption._
import filterAccess.json.{AccessChannelBuilder, UserLevel}
import filterAccess.json.KeyChannelParser._
import filterAccess.persistency.{PermissionPersistency, KeyPersistency}
import filterAccess.tools.DataNaming
import filterAccess.tools.Exceptions._
import nfn.service._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 * Filtering of permissions for GPS tracks (access channel)
 *
 * Access Levels:
 * 0   Full information (no filtering)
 *
 */

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 * Filtering of GPS tracks (access channel)
 *
 */
class AccessChannel extends NFNService {

  private var publicKey: String = "missing public key"
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
   * @param content
   * @return
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

      // cound not parse name
      case _ => None

    }

  }

  /**
   * Pin this service
   */
  override def pinned: Boolean = false // TODO

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
