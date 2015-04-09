package filterAccess.service.content

import akka.actor.ActorRef
import filterAccess.json.{TrackPoint, _}
import filterAccess.tools.Exceptions._
import nfn.service._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
trait ContentChannel extends NFNService {

  private var identity: Int = 237494854
  private var privKey: Int = 0

  /**
   * Set identity (public key) of this service.
   * @param   id    Public Key
   */
  // TODO
  //def setIdentity(id: Int): Unit = {
  //  identity = id
  //}

  /**
   * Get identity (public key) of this service.
   * @return  Public Key
   */
  def getIdentity: Int = identity

  /**
   * Set private key corresponding to public key (identity).
   * @param   id    Public Key
   */
  // TODO
  //def setPrivKey(id: Int): Unit = {
  //  privKey = id
  //}

  /**
   * Get private key corresponding to public key (identity).
   * @return
   */
  def getPrivKey: Int = privKey

  /**
   *
   * @param name
   * @return
   */
  def processContentChannel(name: String, level: Int, ccnApi: ActorRef): Option[String]


  /**
   * Pin this service
   */
  override def pinned: Boolean = false // TODO

  /**
   * Hook up function of this service.
   * @param args Function arguments
   * @param ccnApi Akka Actor
   * @return Execution result
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(name), NFNIntValue(level)) => {
        processContentChannel(name, level, ccnApi) match {
          case Some(t) => NFNStringValue(t)
          case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid access level..")
        }
      }

      //case Seq(NFNContentObjectValue(_, name), NFNIntValue(level)) => {
      //  processContentChannel(new String(name), level) match {
      //    case Some(t) => NFNStringValue(t)
      //    case None => throw new noReturnException("No return. Possibly caused by: Permission denied, invalid access level..")
      //  }
      //}

      case _ =>
        throw new NFNServiceArgumentException(s"ContentChannel: Argument mismatch.")
    }

  }

}
