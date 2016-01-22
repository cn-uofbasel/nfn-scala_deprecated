package nfn.service.GPX

import akka.actor.ActorRef
import ccn.packet.{Interest, CCNName, NFNInterest}
import nfn.service._
import nfn.tools.Networking._

import scala.concurrent.duration._

/**
 * Created by blacksheeep on 20/01/16.
 */
class GPSNearByDetector extends  NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(s1: NFNStringValue, s2: NFNStringValue, ix : NFNIntValue) => {
        val s1name = s1.str.split('/').toList
        val s2name = s2.str.split('/').toList
        val ixi = ix.i

        val pos: List[String] = List(s"p$ixi")
        val s1namePos: List[String] = s1name ++ pos

        val i1 = Interest(CCNName(s1namePos, None)) //TODO check this!!!


        val r1 = new String(fetchContent(i1, ccnApi, 30 seconds).get.data)
        //val timestamp = readTimeStamp

        //val closestOther = findClosestDP(timestamp)

        //val dist = computeDist()

        NFNStringValue(r1.toString)

      }
      case _ =>
        throw new NFNServiceArgumentException(s"Parameter Mismatch")
    }
  }
}

