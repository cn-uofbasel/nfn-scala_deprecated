package nfn.service

import akka.actor.ActorRef

class Translate() extends NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
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
