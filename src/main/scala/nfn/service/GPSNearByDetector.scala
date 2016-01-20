package nfn.service

import akka.actor.ActorRef
import ccn.packet.{NFNInterest, CCNName}
import lambdacalculus.parser.ast.{Constant, Str}
import nfn.tools.Networking._
import scala.concurrent.duration._

/**
 * Created by blacksheeep on 20/01/16.
 */
class GPSNearByDetector extends  NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(s1: NFNStringValue, s2: NFNStringValue, ix : NFNIntValue) => {
        val s1name = s1.str.substring(1).split('/').toList
        val s2name = s2.str.substring(1).split('/').toList
        val ixi = ix.i

        val pos: List[String] = List(s"p$ixi")
        val s1namePos: List[String] = s1name ++ pos
        val i1 = NFNInterest(CCNName(s1namePos, None).toString) //TODO check this!!!

        val r1 = new String(fetchContent(i1, ccnApi, 30 seconds).get.data)

        NFNStringValue(r1.toString)

      }
      case _ =>
        throw new NFNServiceArgumentException(s"Parameter Mismatch")
    }
  }
}

