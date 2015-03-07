package filterAccess.service

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet._
import filterAccess.json.AccessChannelParser._
import filterAccess.json.KeyChannelParser._
import nfn.NFNApi
import nfn.service._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import filterAccess.tools.Networking.fetchContent

import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 * Filtering of GPS tracks (key channel)
 *
 */
class KeyChannel extends NFNService {

  /**
   * Check if certain user is allowed to access certain data at certain level
   * @param data Data extracted from the content object containing the permissions
   * @param node Identifier of the node
   * @param level Access level
   * @return True, if access is allowed. False, if access is denied or json can not be parsed.
   */
  private def checkPermission(data: Array[Byte], node: String, level: Int): Boolean = {

    //parse access level from json
    val real_level = getAccessLevel(new String(data), node)

    // checking permissions
    real_level match {
      case Some(l) => {
        // parsing successful, actual data extraction
        l <= level
      }
      case None => {
        // parsing failed
        false
      }
    }
  }

  /**
   *
   * @param name
   * @param level
   * @param ccnApi
   * @return
   */
  private def getLevelKey(name:String, level:Int, ccnApi:ActorRef):Option[Int] = {

    fetchContent(name, ccnApi, 2 seconds) match {
      case Some(c: Content) => extractLevelKey(new String(c.data), level)
      case _ => ??? // TODO handle future timeout

    }

  }

  /**
   *
   * @param name Name of content object containing the actual data (not keys!).
   * @param user User
   * @param level Access level
   * @param ccnApi Actor Ref
   * @return Key (if allowed)
   */
  private def processKeyTrack(name: String, user: String, level: Int, ccnApi: ActorRef): Option[Int] = {

    fetchContent(name+"/permission", ccnApi, 2 seconds) match {
      case Some(c:Content) => {
        // Ensure permissions
        checkPermission(c.data, user, level) match {
          case true => getLevelKey(name+"/key", level, ccnApi) // access allowed: return key
          case false => None // access denied: no not return key
        }
      }
      case _ => ??? // TODO handle future timeout
    }

  }

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
      case Seq(NFNStringValue(track), NFNStringValue(node), NFNIntValue(level)) => {

        processKeyTrack(new String(track), new String(node), level, ccnApi) match {
          case Some(key) => NFNIntValue(key)
          case None => NFNEmptyValue() // TODO handle permission denied -> No response.
        }
      }

      case _ => {
        throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
      }
    }

  }

}
