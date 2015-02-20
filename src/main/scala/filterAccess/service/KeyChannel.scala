package filterAccess.service

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet._
import filterAccess.json.AccessChannelParser._
import nfn.NFNApi
import nfn.service._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

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
   * @param track
   * @param ccnApi
   * @param time
   * @return
   */
  private def fetchPermissionData(track: String, ccnApi: ActorRef, time:Duration): Option[Content] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    // form interest for permission data
    val i = Interest(CCNName(track.split("/").tail: _*))

    // try to fetch permission data and return if successful
    val futServiceContent: Future[Content] = loadFromCacheOrNetwork(i)
    Await.result(futServiceContent, time) match {
      case c: Content => Some(c)
      case _ => None
    }
  }

  /**
   *
   * @param track
   * @param node
   * @param level
   * @param ccnApi
   * @return
   */
  private def processKeyTrack(track: String, node: String, level: Int, ccnApi: ActorRef): Option[String] = {

    // TODO at "2 seconds" is a compiler warning...
    fetchPermissionData(track, ccnApi, 2 seconds) match {
      case Some(c:Content) => {
        // Ensure permissions
        checkPermission(c.data, node, level) match {
          case true => {
            // access allowed: return key
            Some("this-is-the-secret-key")
          }
          // return key
          case false => {
            // access denied: no not return key
            None
          }
        }
      }
      case _ => ??? // TODO handle future timout
    }

  }

  /**
   * Pin this service
   */
  override def pinned: Boolean = false // TODO

  /**
   * Entry point of this service
   * @param args
   * @param ccnApi
   * @return
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track), NFNStringValue(node), NFNIntValue(level)) => {

        processKeyTrack(new String(track), new String(node), level, ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => NFNEmptyValue() // TODO handle permission denied -> No response.
        }
      }

      case _ => {
        throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
      }
    }

  }

}
