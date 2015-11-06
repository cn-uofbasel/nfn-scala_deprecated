package nfn.service.GPX

import akka.actor.ActorRef
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import nfn.service.GPX.helpers.GPXPointHandler.parseGPXPoint
import nfn.service.GPX.helpers.GPXInterestHandler.fetchRawGPXPoint

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class GPXDistanceComputer extends NFNService {

  def computeDistance(name1:String, n1:Int, name2:String, n2: Int, ccnApi:ActorRef): Option[Double] = {

    // fetch points
    val content1 =  fetchRawGPXPoint(name1, n1, ccnApi)
    val content2 = fetchRawGPXPoint(name2, n2, ccnApi)

    // parse data
    val (lat1, lon1, _) = parseGPXPoint(content1.get)
    val (lat2, lon2, _) = parseGPXPoint(content2.get)

    // compute distance
    // Theory: http://www.kompf.de/gps/distcalc.html
    // TODO: implement improved method
    val dx = 71.5 * (lon1 - lon2)
    val dy = 111.3 * (lat1 - lat2)
    Some(Math.sqrt(dx*dx + dy*dy))

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match{
      case Seq(NFNStringValue(name1), NFNIntValue(n1), NFNStringValue(name2), NFNIntValue(n2)) => {

        computeDistance(name1, n1, name2, n2, ccnApi) match {
          case Some(d) => NFNStringValue(d.toString)
          case None => ???
        }


      }
      case _ => ???

    }
  }

}
