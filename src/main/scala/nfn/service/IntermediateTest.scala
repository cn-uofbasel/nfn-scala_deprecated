package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, NFNInterest}
import nfn.tools.Networking._

import scala.concurrent.duration._
import scala.language.postfixOps

class IntermediateTest() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    for ( i <- 0 to 10) {
      intermediateResult(ccnApi, interestName, i, NFNStringValue("intermediate test " + i))
      Thread.sleep(1 * 1000)
    }
    NFNStringValue("this is the final result")
  }
}

