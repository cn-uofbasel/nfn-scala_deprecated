package orgOpenmhealth.helpers

import ccn.packet._
import akka.actor.ActorRef
import nfn.tools.Networking.fetchContent
import scala.concurrent.duration._



/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/19/16.
  */
object Helpers {

  def buildCatalogName(user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):CCNName =
    CCNName(prefix.split('/').toList, None).append(user).append("data/fitness/physical_activity/time_location/catalog").append(timestamp)

  def buildDataPointPacketName(user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):CCNName =
    CCNName(prefix.split('/').toList, None).append(user).append("data/fitness/physical_activity/time_location").append(timestamp)

  def resolveDataPointPacket(ccnApi: ActorRef, user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1): String = {

    val name = buildDataPointPacketName(user, timestamp)
   new String(fetchContent(Interest(name), ccnApi, 30 seconds).get.data)

  }

  def resolveCatalog(ccnApi: ActorRef, user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):List[String] = {

    val catalogName = buildCatalogName(user, timestamp)
    val catalogData = new String(fetchContent(Interest(catalogName), ccnApi, 30 seconds).get.data)

    val dataPointTimeStamps = catalogData.substring(1,catalogData.length-1).replace(" ", "").split(',')

    (for  { t <- dataPointTimeStamps
      data = resolveDataPointPacket(ccnApi, user, t)
    } yield data).toList
  }



}
