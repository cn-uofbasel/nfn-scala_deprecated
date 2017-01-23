package nfn.service.GPS.GPX

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest}
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import nfn.service.GPS.GPX.helpers.GPXInterestHandler.fetchGPXDistanceComputer


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class GPXDistanceAggregator extends NFNService {

  def aggregateDistance(name:String, n:Int, ccnApi:ActorRef): Option[Double] = {

    var sum = 0.0

    for (i<-2 to n)
      sum += (new String(fetchGPXDistanceComputer(name, i-1, name, i, ccnApi).get.data)).toDouble

    Some(sum)

  }

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match{
      case Seq(NFNStringValue(name), NFNIntValue(n)) => {

        aggregateDistance(name, n, ccnApi) match {
          case Some(d) => NFNStringValue(d.toString) // todo: return as double
          case None => ???
        }

      }
      case _ => ???

    }
  }

}
