package nfn.service.impl

import akka.actor.ActorRef
import nfn.service.{NFNContentObjectValue, NFNService, NFNServiceArgumentException, NFNValue}

class Translate() extends NFNService {

  override def function: (Seq[NFNValue], ActorRef) => NFNValue = { (args, nfnServer) =>
    args match {
      case Seq(NFNContentObjectValue(name, data)) =>
        // translating should happen here
        val str = new String(data)
        NFNContentObjectValue(name, ((str + " ")*3).getBytes)
      case _ =>
        throw new NFNServiceArgumentException(s"Translate service requires a single CCNName as a argument and not $args")
    }
  }
}
