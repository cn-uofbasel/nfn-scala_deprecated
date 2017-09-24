package nfn.service

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking._

import scala.concurrent.duration._


class GetAverageSensorDataService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {

     case Seq(sensortype: NFNStringValue, data: NFNContentObjectValue) => {

       val str = new String(data.data)
       val list = str.split("\n").toList.tail

       val listFiltered = list.filter(_.contains(sensortype.str))

       var sensorValues = listFiltered.map(name => new String(fetchContent(
         new Interest(
           new CCNName(name.split("/").toList.tail, None)
         ),
         ccnApi, 30 seconds).get.data)
       )


       val sensorValuesSeparated =  sensorValues.map(dp => dp.splitAt(2)._1 + "." +  dp.splitAt(2)._2)

       if(sensorValuesSeparated.length == 0) return NFNStringValue("no data")

       val avg = sensorValuesSeparated.foldLeft(0.0)((a,b) => a.toFloat + b.toFloat) / sensorValuesSeparated.length

       NFNStringValue(avg.toString)
      }
     case _ => NFNStringValue("error")
    }
  }
}
