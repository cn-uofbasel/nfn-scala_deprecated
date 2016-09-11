package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

/**
 * Created by basil on 17/06/14.
 */
class NackServ extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    throw new NFNServiceExecutionException("Provoking a nack")
  }
}
