package orgOpenmhealth.services

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest}
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import orgOpenmhealth.helpers.Helpers._

/**
  * Created by Claudio Marxer <claudio.marxer@unibas.ch> on 3/19/16.
  */
class PointCount extends NFNService {


  def countPoint(user:String, starttime:String, ccnApi: ActorRef):NFNIntValue = {

    val points = resolveCatalog(ccnApi, user, starttime)
    NFNIntValue(points.length) // FIXME: actually count points

  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    args match{

      case Seq(NFNStringValue(user), NFNStringValue(starttime)) => {
        countPoint(user:String, starttime:String, ccnApi)
      }

      case _ => ???

    }

  }
}
