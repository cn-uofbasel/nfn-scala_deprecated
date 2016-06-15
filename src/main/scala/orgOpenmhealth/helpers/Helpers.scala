package orgOpenmhealth.helpers

import java.io.{File, PrintWriter}
import java.nio.file.{Paths, Files}

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
    CCNName(prefix.split('/').toList, None).append(user).append(CCNName(List("SAMPLE", "fitness", "physical_activity", "time_location","catalog"), None)).append(timestamp)


  /**
    *
    * @param user
    * @param timestamp
    * @param prefix
    * @param version
    * @return
    */
  def buildDataPointPacketName(user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):CCNName =
    CCNName(prefix.split('/').toList, None).append(user).append(CCNName(List("SAMPLE", "fitness", "physical_activity", "time_location"), None)).append(timestamp)


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


  def requestCatalogTimeStamps(ccnApi: ActorRef, user:String, timestamp:String, prefix:String="org/openmhealth", version:Int = 1):List[String] = {

    val catalogName = buildCatalogName(user, timestamp)
    val catalogData = new String(fetchContent(Interest(catalogName), ccnApi, 30 seconds).get.data)

    val catalogDataTrim = catalogData.replace(" ", "")

    catalogDataTrim.substring(1, catalogDataTrim.length-2).split(',').toList

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
    val dataPointTimeStamps = requestCatalogTimeStamps(ccnApi, user, timestamp, prefix, version)
    (for  { t <- dataPointTimeStamps
      data = resolveDataPointPacket(ccnApi, user, t)
    } yield data).toList
  }

  def contentObjectToByte(name:String, data:String):Array[Byte] = {

    // TODO: better solution..

    // data to file
    val pw = new PrintWriter(new File("/tmp/result"))
    pw.write(data)
    pw.flush()
    pw.close()

    // produce content object
    val ccnLiteEnv = CCNLiteSystemPath.ccnLiteHome
    val mkCmd = s"${ccnLiteEnv}/bin/ccn-lite-mkC -s ndn2013 -i /tmp/result -o /tmp/content-object ${name}"
    mkCmd !

    // return
    //scala.io.Source.fromFile("/tmp/content-object").mkString
    val byteArray = Files.readAllBytes(Paths.get("/tmp/content-object"))
    byteArray
  }


  def timeStampToCatalogTimeStamp(timestamp: String) : String = {
    val fixdate = timestamp.substring(0, 11)
    val min = timestamp.substring(11, 12) + "0"
    val sec = timestamp.substring(13,15)

    fixdate + min + "00"
  }


}
