package orgOpenmhealth.helperServices

import akka.actor.ActorRef
import nfn.service._
import orgOpenmhealth.helpers.Helpers._

/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/21/16.
  */
class SimpleToJSON extends NFNService {


  def convert(simple:String):String = {
    // TODO: this function ignores time stamps
    val lat = simple.split(',')(0)
    val lng = simple.split(',')(1)
    s"{\"lat\":${lat},\"lng\":${lng}"
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    /*
     *
     * Convert coordinates in the following form..
     *    34.076513,-118.439802
     * ..to following form (JSON):
     *    {"lat":34.076513,"lng":-118.439802}
     */

    args match {

      case simple:NFNContentObjectValue => NFNStringValue(convert(new String(simple.data)))
      case Seq(NFNStringValue(simple)) => NFNStringValue(convert(simple))
      case _ => ???

    }

  }

}
