package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

class Echo() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    NFNDataValue(args.map(value => value.toDataRepresentation).reduceLeft(_ ++ _))
  }
}

