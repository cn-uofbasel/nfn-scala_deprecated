package orgOpenmhealth.services

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content}
import config.CCNLiteSystemPath
import nfn.service
import nfn.service._
import orgOpenmhealth.helpers.Helpers._

/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/21/16.
  */
class DistanceTo extends NFNService {


  def computeDistanceTo(user:String, point:String, time:String, ccnApi: ActorRef):Int = {

    
    //fetch corresponding catalog
    val points = requestCatalogTimeStamps(ccnApi, user, timeStampToCatalogTimeStamp(time))
    if(points.contains(time)){ //not exact matching required!
       val coordinates = resolveDataPointPacket(ccnApi, user, time)
      return 1
    }
    //search for point near by given timestamp
    //replie result
    return 0
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match{

      case Seq(NFNStringValue(user), NFNStringValue(point), NFNStringValue(time)) => {
        // compute result
        val result =computeDistanceTo(user, point, time, ccnApi)
        //encapsulate
        val innerName = s"/org/openmhealth/$user/Data/fitness/physical_activity/genericfunction/TODO/PREFIX/DistanceTo/$point/$time"
        val innerContentObject = contentObjectToByte(innerName, result.toString)

        NFNDataValue(innerContentObject)
      }

      case _ => ???

    }

  }

}
