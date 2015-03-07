package filterAccess.service

import akka.actor.ActorRef
import nfn.service._

import filterAccess.json._
import filterAccess.json.TrackPoint

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 *  Filtering of GPS tracks (content channel)
 *
 * Access Levels:
 *  -1   No permissions
 *   0   Raw data (no filtering)
 *   1   Shift a track so that the starting point lies
 *       on the origin of the coordinate system.
 *       (northpole filtering)
 *
 */
class ContentChannel extends NFNService {

  private def processFilterTrack(track: String, level: Int): Option[String] = {

    // extract trace and name
    val trace = ContentChannelParser.getTrace(track)
    val name = ContentChannelParser.getName(track)

    (trace, name) match {
      case (Some(t), Some(n)) => {
        // actual filtering
        val offset = level match {
          case 0 => new TrackPoint(0, 0, 0)
          case 1 => t(0)
          case _ => throw new NFNServiceArgumentException(s"Invalid access level.")
        }
        val filtered_trace = (for (point <- t) yield point - offset).toList
        
        // rebuild json
        Some {
          ContentChannelBuilder.buildTrack(filtered_trace, n)
        }

      }
      case _ => None
    }

  }

  /**
   * Pin this service
   */
  override def pinned: Boolean = false // TODO

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track), NFNIntValue(level)) => {
        processFilterTrack(track, level) match {
          case Some(t) => NFNStringValue(t)
          case None => ??? // TODO
        }
      }

      case Seq(NFNContentObjectValue(_, track), NFNIntValue(level)) => {
        processFilterTrack(new String(track), level) match {
          case Some(t) => NFNStringValue(t)
          case None => ??? // TODO
        }
      }

      case _ =>
        throw new NFNServiceArgumentException(s"ContentChannel: Argument mismatch.")
    }

  }

}
