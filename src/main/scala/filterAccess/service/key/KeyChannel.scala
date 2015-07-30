package filterAccess.service.key

import filterAccess.service.Channel
import nfn.service._
import akka.actor.ActorRef

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
   * @param    rdn       Relative data name
   * @param    level     Access level
   * @param    id        User Identity (PubKey)
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  def processKeyChannel(rdn: String, level: Int, id: String, ccnApi: ActorRef): Option[String]


  /** Set a custom name for this Service */
  // override def ccnName: CCNName = CCNName("key")

  /** Pin this service */
  override def pinned: Boolean = false

  /**
   * Entry point of this service.
   *
   * @param    args     Function arguments
   * @param    ccnApi   Akka Actor
   * @return            Functions result
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(rdn), NFNIntValue(level), NFNStringValue(pubKey)) => {
        processKeyChannel(new String(rdn), level, new String(pubKey), ccnApi) match {
          case Some(key) => {
            // TODO
            // If the first character is a number just parts of the string is returned
            // -> CCNLite/NFN-Scala Bug?
            NFNStringValue(key)
          }
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid rdn or ran")
        }
      }

      case _ => {
        throw new NFNServiceArgumentException(s"KeyChannel Service: Argument mismatch.")
      }
    }

  }

}
