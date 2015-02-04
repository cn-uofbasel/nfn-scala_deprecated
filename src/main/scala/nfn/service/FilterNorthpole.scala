package nfn.service

import akka.actor.ActorRef

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 */
class FilterNorthpole extends NFNService {

  def processNorthpole(track:String):String = {
    // TODO implement actual filtering
    "Aktual filtering not implemented. Input track was: " + track
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match {
      case Seq(NFNContentObjectValue(_, track)) =>
        NFNStringValue(new String(track))

      case Seq(NFNStringValue(track)) =>
        NFNStringValue(processNorthpole(track))

      case _ =>
        throw new NFNServiceArgumentException(s"Argument mismatch.")
    }

  }

}
