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

  // Setting publicKey via setPublicKey(...) does cause problems
  // TODO - Why?
  // Workaround: Hard-code this in here..

  /** public key (identity) */
  private var publicKey: String = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ=="

  /** corresponding private key */
  private var privateKey: String = "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="

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
