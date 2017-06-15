package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking.{fetchRequestsToComputation, intermediateResult}

class Waypoint() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    if (args.length != 1) {
      throw NFNServiceArgumentException(s"$ccnName takes a single string argument.")
    }

    val name = args.head match {
      case NFNStringValue(s) => s
      case _ => throw NFNServiceArgumentException(s"$ccnName takes a single string argument.")
    }

    var currentIndex = 0
    while (true) {
      println(s"Fetching local content for $interestName")
      fetchRequestsToComputation(Interest(interestName), ccnApi) match {
        case Some(request) =>
          request.requestParameters.headOption match {
            case Some(parameter) =>
              val prefix = parameter.replace("%2F", "/")
              intermediateResult(ccnApi, interestName, currentIndex, NFNStringValue(prefix))
              currentIndex += 1
            case None => println("No prefix specified in request. Ignoring.")
          }
        case None => println("No requests to computation found.")
      }
      Thread.sleep(1000)
    }
    NFNIntValue(1)
  }
}

