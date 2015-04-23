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
 * Used implement classes to set up the services for the key channel (storage as well as proxy).
 *
 */
abstract class KeyChannel extends Channel {


  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    content   Raw data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  def processKeyChannel(name: String, level: Int, id: String, ccnApi: ActorRef): Option[String]


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
      case Seq(NFNStringValue(name), NFNIntValue(level), NFNStringValue(pubKey)) => {
        processKeyChannel(new String(name), level, new String(pubKey), ccnApi) match {
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
