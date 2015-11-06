package nfn.service.GPX

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet.{CCNName, Content, Interest}
import nfn.NFNApi
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import filterAccess.tools.Networking.fetchContent

import scala.concurrent.{Await, Future}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
class GPXDistanceAggregator extends NFNService {

  def aggregateDistance(name:String, n:Int, ccnApi:ActorRef): Option[Double] = {
    ???
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
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
