package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

import scala.util.{Try, Failure, Success}
import ccn.packet._
import nfn.NFNApi

import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

import filter_access.json.AccessChannelParser._

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
   * @param node
   * @param level
   * @param ccnApi
   * @return
   */
  private def processKeyTrack(track: String, node: String, level: Int, ccnApi: ActorRef): Option[String] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(2000)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    // form interest for permission data
    val i = Interest(CCNName(track.split("/").tail: _*))

    // try to fetch permission data
    val futServiceContent: Future[Content] = loadFromCacheOrNetwork(i)
    Await.result(futServiceContent, 2 seconds) match {
      // TODO with "2 seconds" is a compiler warning...
      // If content object with permissions received...
      case c: Content => {
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
    }

    // TODO handle future timeout

  }

  /**
   * Entry point of that service
   * @param args
   * @param ccnApi
   * @return
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track), NFNStringValue(node), NFNIntValue(level)) => {

        processKeyTrack(new String(track), new String(node), level, ccnApi) match {
          case Some(key) => NFNStringValue(key)
          case None => NFNEmptyValue() // TODO handle permission denied
        }
      }

      case _ => {
        throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
      }
    }

  }

}
