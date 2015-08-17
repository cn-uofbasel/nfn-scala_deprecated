package filterAccess.service.content

import filterAccess.service.Channel
import nfn.service._
import akka.actor.ActorRef

import filterAccess.tools.Exceptions._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Used implement classes to set up the services for the content channel (processing and storage as well as proxy).
 *
 */
abstract class ContentChannel extends Channel {


  /**
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn      Relative data name
   * @param    level    Access level
   * @param    ccnApi   Akka Actor
   * @return            JSON Object
   */
  def processContentChannel(rdn: String, level: Int, ccnApi: ActorRef): Option[String]


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
      case Seq(NFNStringValue(rdn), NFNIntValue(level)) => {
        processContentChannel(rdn, level, ccnApi) match {
          case Some(t) => NFNStringValue(t)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid rdn or ran")
        }
      }
      case _ =>
        throw new NFNServiceArgumentException(s"ContentChannel: Argument mismatch.")
    }

  }

}
