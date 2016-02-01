package nfn.service.NDNfit

import akka.actor.ActorRef
import nfn.service.GPX.helpers.GPXInterestHandler.fetchRawGPXPoint
import nfn.service.GPX.helpers.GPXPointHandler._
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}


class OriginFilter extends NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head) match{
      case (timestamp: NFNIntValue) => {

        // todo
        ???

      }

      case _ => ???

    }
  }
}
