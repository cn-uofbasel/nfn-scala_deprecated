package nfn.service.GPX

import akka.actor.ActorRef
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import nfn.service.GPX.helpers.GPXPointHandler._
import nfn.service.GPX.helpers.GPXInterestHandler.fetchRawGPXPoint

/**
 * Created by blacksheeep on 22/10/15.
 */
class GPXOriginFilter extends NFNService {

  def north_pole_filter(p1: GPXPoint, pn: GPXPoint): GPXPoint = {
    (pn._1 - p1._1, pn._2 - p1._2, pn._3)
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head) match{
      case (name: NFNStringValue, num: NFNIntValue) => {

        //Request the input data
        val inputdata = fetchRawGPXPoint(name.str, num.i, ccnApi).get
        val refdata = fetchRawGPXPoint(name.str, 1, ccnApi).get

        //Parse Data
        val p_req = parseGPXPoint(inputdata)
        val p_ref = parseGPXPoint(refdata)

        val filtered = north_pole_filter(p_ref, p_req)

        NFNStringValue(createXmlGPXPoint(filtered))
      }
      case _ => ???

    }
  }
}
