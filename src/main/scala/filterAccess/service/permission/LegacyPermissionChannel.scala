package filterAccess.service.permission

import akka.actor.ActorRef
import filterAccess.json.{PermissionChannelBuilder, UserLevel}
import filterAccess.tools.Exceptions._
import nfn.service._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 *
 *   !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 *    DO NOT WORK WITH THIS CLASS!
 *    THIS FILE IS JUST HOLD TO KEEP OLD RUNNABLES WORKING
 *
 *   !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *
 * Filter:
 * Filtering of permissions for GPS tracks (permission channel)
 *
 * Access Levels:
 * 0   Full information (no filtering)
 *
 */

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 * Filtering of GPS tracks (permission channel)
 *
 */
@deprecated("This was a first prototype", "unknown")
class LegacyPermissionChannel extends NFNService {

  private def processPermissionTrack(request: String, level: Int): String = {

    val permissionData = PermissionChannelBuilder.buildPermissions(
      List(
        UserLevel("user1", 0),
        UserLevel("user2", 1),
        UserLevel("processor", 0)),
      "/node/node1/permissionTrack"
    ).getBytes

    level match {
      case 0 => {
        // TODO - take these information from database
        // TODO - more expressive requests
        request match {
          case "user1 track" => "0" // full access
          case "user2 track" => "1" // northpole filter
          case "user3 track" => "-1" // no permissions
          case "processor track" => "0" // full access
          case _ => throw new noReturnException("No response because permission denied.")
        }

      }

      case _ =>
        throw new NFNServiceArgumentException(s"Invalid access level.")
    }

  }

  /**
   * Pin this service
   */
  override def pinned: Boolean = false

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(request), NFNIntValue(level)) =>
        NFNStringValue(processPermissionTrack(request, level))

      case Seq(NFNContentObjectValue(_, request), NFNIntValue(level)) =>
        NFNStringValue(processPermissionTrack(new String(request), level))

      case _ =>
        throw new NFNServiceArgumentException(s"LegacyPermissionChannel: Argument mismatch.")
    }

  }

}
