package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.io.Source


class SensorDataProcessingService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
     case Seq(num: NFNIntValue, data: NFNStringValue) => {
       val list = data.str.split("\n")
       NFNStringValue(list.toList(num.i))
      }
    }
  }
}
