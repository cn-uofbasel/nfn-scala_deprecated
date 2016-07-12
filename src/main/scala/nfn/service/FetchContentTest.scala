package nfn.service

import akka.actor.ActorRef
import ccn.packet.NFNInterest
import nfn.tools.Networking._

import scala.concurrent.duration._
import scala.language.postfixOps

class FetchContentTest() extends NFNService {
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
//    def splitString(s: String) = s.split(" ").size

    val r = new String(fetchContentAndKeepAlive(NFNInterest("call 2 /node/nodeF/nfn_service_DelayedWordCount 'foo bar'"), ccnApi, 3 seconds).get.data).toInt
    NFNIntValue(r)
  }
}

