package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

/**
 * Simple service which takes a sequence of [[NFNIntValue]] and sums them to a single [[NFNIntValue]]
 */
class SumService() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    NFNIntValue(
      args.map({
        case i: NFNIntValue => i.i
        case _ => throw  new NFNServiceArgumentException(s"SumService requires a sequence of NFNIntValue's and not $args")
      }).sum
    )
  }
}
