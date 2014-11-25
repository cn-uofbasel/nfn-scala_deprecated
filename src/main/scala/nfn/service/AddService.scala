package nfn.service

import akka.actor.ActorRef


class AddService() extends  NFNService {

  override def function: (Seq[NFNValue], ActorRef) => NFNValue = { (values: Seq[NFNValue], _) =>
    values match {
      case Seq(l: NFNIntValue, r: NFNIntValue) => {
        NFNIntValue(l.i + r.i)
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName requires to arguments of type NFNIntValue and not $values")
    }
  }
}
