package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.io.Source


class SensorDataProcessingService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    //return NFNStringValue(args.tail.head.getClass.toString)
    args match {

     case Seq(num: NFNIntValue, data: NFNContentObjectValue) => {
       val list = new String(data.data).split("\n")
       NFNStringValue(list.toList(num.i))
      }
     case _ => NFNStringValue("error")
    }
  }
}
