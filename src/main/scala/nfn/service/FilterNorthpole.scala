package nfn.service

import akka.actor.ActorRef

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 *   Shift a track so that the starting point lies on the origin of the coordinate system.
 *
 */


class FilterNorthpole extends NFNService {

  def processNorthpole(track:String):String = {

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
    " " + coordinates.mkString(" ") // TODO Problem when whitespace is missing!

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNStringValue(track)) =>
        NFNStringValue(processNorthpole(track))

      case Seq(NFNContentObjectValue(_, track)) =>
        NFNStringValue(new String(track))

      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}
