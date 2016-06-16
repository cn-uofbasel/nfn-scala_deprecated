package orgOpenmhealth.services

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content}
import config.CCNLiteSystemPath
//import net.named_data.jndn.util.Blob
//import net.named_data.jndn.{Name, Face}
//import net.named_data.jndn.encrypt.{ConsumerDb, Consumer}
//import net.named_data.jndn.security.KeyChain
import nfn.service
import nfn.service._
import orgOpenmhealth.helpers.Helpers._




/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/21/16.
  */
class DistanceTo extends NFNService {


  def computeDistanceTo(user:String, point:String, time:String, ccnApi: ActorRef):Int = {

    
    //fetch corresponding catalog //FIXME Reenable catalog files, but not available on server
    val points = requestCatalogTimeStamps(ccnApi, user, timeStampToCatalogTimeStamp(time))
    if(points.contains(time)){ //not exact matching required!
       val coordinates = resolveDataPointPacket(ccnApi, user, time)
      return 1
    }
    val coordinates = resolveDataPointPacket(ccnApi, user, time)

    //search for point near by given timestamp
    //replie result
    //TODO compute the actual distance
    return 1
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match{

      case Seq(NFNStringValue(user), NFNStringValue(point), NFNStringValue(time)) => {
        // compute result
        val result = computeDistanceTo(user, point, time, ccnApi)
        NFNDataValue(result.toString.getBytes())
        //encapsulate
        //val innerName = s"/org/openmhealth/$user/data/fitness/physical_activity/genericfunction/DistanceTo/$point/$time"
        //val innerContentObject = contentObjectToByte(innerName, result.toString)

        //NFNDataValue(innerContentObject)


      }

      case _ => ???

    }

  }

}
