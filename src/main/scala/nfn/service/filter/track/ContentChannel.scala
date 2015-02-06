package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

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
 *      on the origin of the coordinate system.
 *      (northpole filtering)
 *
 */


class ContentChannel extends NFNService {

  private def processFilterTrack(track:String, level:Int):String = {

    // convert from string to int array
    val coordinates = track.split(" ").map(_.toInt)

    // set offset dependent on access level
    val offset:List[Int] = level match {
      case 0 =>
        List(0,0,0)
      case 1 =>
        List(-coordinates(0), -coordinates(1), -coordinates(2))
      case _ =>
        throw new NFNServiceArgumentException(s"Invalid access level.")
    }

    // shift x-coordinates
    for (i <- 0 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset(0)
    }

    // shift y-coordinates
    for (i <- 1 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset(1)
    }

    // shift z-coordinates
    for (i <- 2 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset(2)
    }

    // convert back to string
    " " + coordinates.mkString(" ")
    // TODO FIX
    // Problem when whitespace is missing!
    // Occurs, then returned string starts with a number.

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track), NFNIntValue(level)) =>
        NFNStringValue(processFilterTrack(track,level))

      case Seq(NFNContentObjectValue(_, track), NFNIntValue(level)) =>
        NFNStringValue(processFilterTrack(new String(track),level))

      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}
