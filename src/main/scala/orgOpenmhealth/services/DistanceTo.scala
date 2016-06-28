package orgOpenmhealth.services

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest}
import config.CCNLiteSystemPath
import nfn.tools.Networking._
//import net.named_data.jndn.util.Blob
//import net.named_data.jndn.{Name, Face}
//import net.named_data.jndn.encrypt.{ConsumerDb, Consumer}
//import net.named_data.jndn.security.KeyChain
import nfn.service
import nfn.service._
import orgOpenmhealth.helpers.Helpers._
import scala.concurrent.duration._



/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/21/16.
  */
class DistanceTo extends NFNService {


  def computeDistanceTo(user:String, point:String, time:String, ccnApi: ActorRef):Double = {

    
    //fetch corresponding catalog
    val points = requestCatalogTimeStamps(ccnApi, user, timeStampToCatalogTimeStamp(time))
    if(points.contains(time)){ //not exact matching required!
       val coordinates = resolveDataPointPacket(ccnApi, user, time)

       val lat = coordinates.split(""""lat":""").tail.head.split(""",""").head.toInt
       val lng = coordinates.split(""""lng":""").tail.head.split("""}""").head.toInt

       val refname = new CCNName(point.split("/").toList, None)
       val catalogData = new String(fetchContent(Interest(refname), ccnApi, 30 seconds).get.data)

       val reflat = catalogData.split("""lat="""").tail.head.split(""""""").head.toInt
       val reflng = catalogData.split("""lon="""").tail.head.split(""""""").head.toInt

       val dx = 71.5 * (lng - reflng)
       val dy = 111.3 * (lat - reflat)

      return Math.sqrt(dx*dx + dy*dy)

    }




    //search for point near by given timestamp
    //replie result
    //TODO compute the actual distance
    return -1
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match{

      case Seq(NFNStringValue(user), NFNStringValue(point), NFNStringValue(time)) => {
        // compute result
        val result = computeDistanceTo(user, point, time, ccnApi)
        //NFNDataValue(result.toString.getBytes())
        NFNFloatValue(result)
        //encapsulate
        //val innerName = s"/org/openmhealth/$user/data/fitness/physical_activity/genericfunction/DistanceTo/$point/$time"
        //val innerContentObject = contentObjectToByte(innerName, result.toString)

        //NFNDataValue(innerContentObject)


      }

      case _ => ???

    }

  }

}
