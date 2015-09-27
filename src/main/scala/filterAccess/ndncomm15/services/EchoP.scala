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
  val aclLoc = "/home/claudio/acl"

  /**
   * Look a symmetric key in persistent storage
   *
   * @param   pubKey  Requesters public key
   * @return          Symmetric key or none
   */
  def lookupKey(pubKey:String): Option[String] = {
    try {
      // run command to extract value from config file
      Seq( "/bin/sh", "-c",
        s"grep '^$pubKey,.*' $aclLoc |" +
          "tail -1"
      ) !! match {
        // execution successful and result not empty
        case value: String if value.length > 0 => Some(value.trim)
        // execution successful but empty result
        case _ => None
      }

    }
    catch {
      // execution failed. config file not found?
      case _:Throwable => None
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
    fw.write(pubKey + "," + symKey + "\n")
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
