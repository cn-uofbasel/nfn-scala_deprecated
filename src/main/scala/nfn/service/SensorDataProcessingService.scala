package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking._
import scala.concurrent.duration._

import scala.io.Source


class SensorDataProcessingService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    //return NFNStringValue(args.tail.head.getClass.toString)
    args match {

     case Seq(num: NFNIntValue, data: NFNContentObjectValue) => {
       val list = new String(data.data).split("\n").toList

       //request data from remote host here
       val i1 = new Interest( new CCNName(list(num.i).split("/").toList.tail, None))
       val c1 = fetchContent(i1, ccnApi, 30 seconds).get

       val cont = new String(c1.data)
       //val cont = list(num.i)
       NFNStringValue(cont)
      }
     case _ => NFNStringValue("error")
    }
  }
}
