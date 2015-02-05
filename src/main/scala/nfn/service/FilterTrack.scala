package nfn.service

import akka.actor.ActorRef

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Access Levels:
 *  0   Raw data (no filtering)
 *  1   Shift a track so that the starting point lies
 *      on the origin of the coordinate system.
 *      (northpole filtering)
 *
 */


class FilterTrack extends NFNService {

  private def processFilterTrack(track:String):String = {

    // convert from string to int array
    val coordinates = track.split(" ").map(_.toInt)

    // offset
    val offset_x = -coordinates(0)
    val offset_y = -coordinates(1)
    val offset_z = -coordinates(2)

    // shift x-coordinates
    for (i <- 0 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset_x
    }

    // shift y-coordinates
    for (i <- 1 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset_y
    }

    // shift z-coordinates
    for (i <- 2 until coordinates.length by 3) {
      coordinates(i) = coordinates(i) + offset_z
    }

    // convert back to string
    " " + coordinates.mkString(" ")
    // TODO
    // Problem when whitespace is missing!
    // Occurs, then returned string starts with a number.

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track)) =>
        NFNStringValue(processFilterTrack(track))

      case Seq(NFNContentObjectValue(_, track)) =>
        NFNStringValue(processFilterTrack(new String(track)))

      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}
