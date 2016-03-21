package orgOpenmhealth.services

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content}
import config.CCNLiteSystemPath
import nfn.service._
import orgOpenmhealth.helpers.Helpers._

/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/21/16.
  */
class DistanceTo extends NFNService {


  def computeDistanceTo(user:String, point:String, time:String, ccnApi: ActorRef):NFNIntValue = {

    
    ???

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match{

      case Seq(NFNStringValue(user), NFNStringValue(point), NFNStringValue(time)) => {
        // compute result
        val result =computeDistanceTo(user, point, time, ccnApi).toDataRepresentation
        //encapsulate
        val innerName = s"/org/openmhealth/$user/Data/fitness/physical_activity/genericfunction/TODO/PREFIX/DistanceTo/$point/$time"
        import ccn.ccnlite.CCNLiteInterfaceCli._
        val innerContent = ???
        NFNDataValue(innerContent)
      }

      case _ => ???

    }

  }

}
