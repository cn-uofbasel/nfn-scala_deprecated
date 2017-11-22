package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest, NFNInterest}
import nfn.tools.Networking._

import scala.concurrent.Await
import scala.concurrent.duration._


class ChainIntermediates() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    if (args.length < 2) {
      throw new NFNServiceArgumentException(s"$ccnName requires 2 arguments")
    }

//    val simulationCall = args(0).asInstanceOf[NFNStringValue].str.stripPrefix("/")
    val simulationCall = CCNName("node6" :: "nfn_service_IntermediateTest" :: "(@x call 1 x)" :: "NFN" :: Nil, None)
    val renderCall = args(1).asInstanceOf[NFNStringValue].str.stripPrefix("/")


    val simulationInterest = Interest(simulationCall)
    val renderInterest = Interest(renderCall)

    //println(s"Simulation Call: ${simulationCall.toString}")

    val intermediateHandler = (index: Int, content: Content) => {
      val c = new String(content.data)
      println(s"intermediate handler: $index, $c")
    }
    fetchContentAndKeepalive(ccnApi, simulationInterest, 20 seconds, Some(intermediateHandler))

//    val futureContent = requestContentAndKeepalive(ccnApi, simulationInterest)
//    requestIntermediates(ccnApi, simulationInterest, handleIntermediates = { (c: Content) =>
//
//    })



    //fetchContentAndKeepalive(ccnApi, simulationInterest, 20 seconds) Some((intermediate) => ()))

    //fetchContentAndKeepAlive(renderInterest)

    //NFNDataValue(fetchContentAndKeepAlive(NFNInterest(s"(call 1 /node6/nfn_service_NBody_SimulationService)"), ccnApi, 20 seconds).get.data)
    NFNIntValue(3)
  }
}

