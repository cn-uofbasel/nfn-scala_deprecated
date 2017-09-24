package nfn.service

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._




class GetListedSensorDataService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    //return NFNStringValue(args.tail.head.getClass.toString)
    args match {

     case Seq(sensortype: NFNStringValue, data: NFNContentObjectValue) => {


       val list = new String(data.data).split("\n").toList.tail

       val listFiltered = list.filter(_.contains(sensortype.str))

       var sensorValues = listFiltered.map(name => new String(fetchContent(
         new Interest(
           new CCNName(name.split("/").toList.tail, None)
         ),
         ccnApi, 30 seconds).get.data)
       )

       val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm")

       val timestams = listFiltered.map(uxTS => "\"" + sdf.format(Date.from(
                  Instant.ofEpochSecond(uxTS.split("/").last.toInt))) + "\"" )

       val sensorValuesSeparated =  sensorValues.map(dp => dp.splitAt(2)._1 + "." +  dp.splitAt(2)._2)

       val content = "{ \"x\" : [" + timestams.mkString(",") + "] , \"y\": [" + sensorValuesSeparated.mkString(",") + "] }"
       //val cont = list(num.i)


       NFNStringValue(content)
      }
     case _ => NFNStringValue("error")
    }
  }
}
