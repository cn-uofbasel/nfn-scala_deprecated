package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking._
import scala.concurrent.duration._

class PubSubBroker() extends NFNService {
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
      val components = name.stripPrefix("/").split("/").toList map {
        component => component.replace("%2F", "/")
      }
//      throw NFNServiceArgumentException("TEST: " + components)
      val interest = Interest(CCNName(components :+ currentIndex.toString, None))
      fetchContent(interest, ccnApi, 20 seconds) match {
        case Some(content) => intermediateResult(ccnApi, interestName, currentIndex, NFNDataValue(content.data))
        case None => println("No content found. Try again.")
        //      }
      }
      currentIndex += 1
    }
    NFNIntValue(1)
  }
}

