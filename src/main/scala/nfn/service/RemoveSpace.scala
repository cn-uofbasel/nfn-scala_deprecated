package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

/**
 * Created by blacksheeep on 01.12.14.
 */
class RemoveSpace extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(NFNStringValue(arg1)) => NFNStringValue(arg1.filter(_ != ' '))
      case _ => ???
    }
  }
}
