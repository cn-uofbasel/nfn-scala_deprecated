package nfn.service

import akka.actor.ActorRef


class MinusService() extends  NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(l: NFNIntValue, r: NFNIntValue) => {
        val res = NFNIntValue(l.i - r.i)
        return res
      }
      case Seq(l: NFNFloatValue, r: NFNFloatValue) => {
        val res = NFNFloatValue(l.f - r.f)
        return res
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName requires to arguments of type NFNIntValue and not $args")
    }
  }
}
