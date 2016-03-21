package orgOpenmhealth.helpers

import ccn.packet._
import akka.actor.ActorRef
import config.CCNLiteSystemPath
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}
import nfn.service.NFNValue
import nfn.tools.Networking.fetchContent
import scala.concurrent.duration._
import sys.process._


/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/19/16.
  */
object Helpers {


  /**
    *
    * @param user
    * @param timestamp
    * @param prefix
    * @param version
    * @return
    */
  def buildCatalogName(user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):CCNName =
    CCNName(prefix.split('/').toList, None).append(user).append(CCNName(List("data", "fitness", "physical_activity", "time_location","catalog"), None)).append(timestamp)


  /**
    *
    * @param user
    * @param timestamp
    * @param prefix
    * @param version
    * @return
    */
  def buildDataPointPacketName(user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):CCNName =
    CCNName(prefix.split('/').toList, None).append(user).append(CCNName(List("data", "fitness", "physical_activity", "time_location"), None)).append(timestamp)


  /**
    *
    * @param ccnApi
    * @param user
    * @param timestamp
    * @param prefix
    * @param version
    * @return
    */
  def resolveDataPointPacket(ccnApi: ActorRef, user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1): String = {

    val name = buildDataPointPacketName(user, timestamp)
   new String(fetchContent(Interest(name), ccnApi, 30 seconds).get.data)

  }


  /**
    *
    * @param ccnApi
    * @param user
    * @param timestamp
    * @param prefix
    * @param version
    * @return
    */
  def resolveCatalog(ccnApi: ActorRef, user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):List[String] = {

    val catalogName = buildCatalogName(user, timestamp)
    val catalogData = new String(fetchContent(Interest(catalogName), ccnApi, 30 seconds).get.data)

    val dataPointTimeStamps = catalogData.substring(1,catalogData.length-1).replace(" ", "").split(',')

    (for  { t <- dataPointTimeStamps
      data = resolveDataPointPacket(ccnApi, user, t)
    } yield data).toList
  }

  def contentObjectToByte(name:String, data:Any):Array[Byte] = {

    // TODO: better solution..

    // data to file
    val writeCmd = "echo '" + data + "'" #> "/tmp/result"
    writeCmd !

    // produce content object
    val ccnLiteEnv = CCNLiteSystemPath.ccnLiteHome
    val mkCmd = List("$./ccnLiteEnv/bin/ccn-lite-mkC ${c.name.toString} -s ndn2013 -i /tmp/result") #> "/tmp/content-object"
    mkCmd !

    // return
    scala.io.Source.fromFile("/tmp/content-object").map(_.toByte).toArray

  }


}
