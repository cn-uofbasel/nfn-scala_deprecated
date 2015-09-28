package filterAccess.ndncomm15.services

import akka.actor.ActorRef
import filterAccess.crypto.Helpers.symKeyGenerator
import filterAccess.tools.Exceptions._
import nfn.service._

import scala.util.Random
import java.io.FileWriter
import scala.sys.process._


class EchoP extends NFNService {

  /* ACL (access control list) location in file system */
  val aclLoc = "ac.txt"

  /**
   * Look a symmetric key in persistent storage
   *
   * @param   pubKey  Requesters public key
   * @return          Symmetric key or none
   */
  def lookupKey(pubKey:String): Option[String] = {
    val lines = scala.io.Source.fromFile(aclLoc).getLines()

    val split_lines = lines.map(x => x.split(","))

    val keyline: Option[Array[String]] = split_lines.find(x=> x.head == pubKey)

    keyline match {
      case Some(key) => Some(key.tail.tail.head)
      case _ => None
    }
  }

  /**
   * Generate a new key and store persistently
   * $
   * @param   pubKey  Requesters public key
   * @return          Symmetric key or none
   */
  def generateKey(pubKey:String): String = {

    // generate a new key
    val r = (new Random).nextString(256)
    val symKey = symKeyGenerator(r)

    // make persistent
    val fw = new FileWriter(aclLoc, true)
    fw.write(pubKey +"," + "k" + "," + symKey + "\n")
    fw.close

    // return
    symKey
  }

  /**
   * Permission checking
   *
   * @param   pubKey  Requesters public key
   * @return          Authorized?
   */
  def permissionChecking(pubKey:String): Boolean = {
    true // TODO -- implement better permission checking. :-)
  }

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    pubKey    Requesters public key
   * @return             Symmetric key (asymmentrically encryption) or none.
   */
  def processEcho(pubKey: String): Option[String] = {

    // permission checking
    permissionChecking(pubKey) match {
      case true => {
        // authorized
        lookupKey(pubKey) match {
          case Some(persistentKey) => Some(persistentKey)
          case None => Some(generateKey(pubKey))
        }
      }
      // permission denied
      case false => {
        None
      }
    }
  }

  /**
   * Entry point of this service.
   *
   * @param    args     Function arguments
   * @param    ccnApi   Akka Actor
   * @return            Functions result
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(pubKey)) => processEcho(pubKey) match {
        case Some (res) => NFNStringValue (res)
        case None => throw new noReturnException ("No return. Most possibly caused by: Permission denied.")
      }
      case _ => throw new noReturnException("No return. Caused by: Argument missmatch.")
    }

  }

}
