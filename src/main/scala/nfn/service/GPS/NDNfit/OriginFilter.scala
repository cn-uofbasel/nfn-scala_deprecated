package nfn.service.GPS.NDNfit

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}


class OriginFilter extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head) match{
      case (timestamp: NFNIntValue) => {

        // todo
        ???

      }

      case _ => ???

    }
  }
}
