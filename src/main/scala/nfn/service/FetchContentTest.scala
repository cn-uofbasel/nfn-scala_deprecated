package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, NFNInterest}
import nfn.tools.Networking._

import scala.concurrent.duration._
import scala.language.postfixOps

class FetchContentTest() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    //val r = new String(fetchContentAndKeepAlive(NFNInterest("call 2 /node/nodeF/nfn_service_DelayedWordCount 'foo bar'"), ccnApi, 3 seconds).get.data).toInt
    //NFNIntValue(r)

    args.head match {
      case NFNStringValue(s) => NFNDataValue(fetchContentAndKeepAlive(NFNInterest(s"(call 2 /node/nodeF/nfn_service_DelayedWordCount '${s}')"), ccnApi, 3 seconds).get.data)
      case NFNIntValue(s) => NFNDataValue(fetchContentAndKeepAlive(NFNInterest(s"(call 2 /node/nodeF/nfn_service_DelayedWordCount ${s})"), ccnApi, 3 seconds).get.data)
      case _ => NFNIntValue(0)
    }
  }
}

