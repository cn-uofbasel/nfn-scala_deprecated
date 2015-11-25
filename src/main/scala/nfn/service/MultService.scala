package nfn.service

import akka.actor.ActorRef


class MultService() extends  NFNService {

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(l: NFNIntValue, r: NFNIntValue) => {
        NFNFloatValue(l.i * r.i)
      }
      case Seq(l: NFNFloatValue, r: NFNFloatValue) => {
        NFNFloatValue(l.f * r.f)
      }
      case Seq(l: NFNIntValue, r: NFNContentObjectValue) => {
        NFNFloatValue(l.i.toFloat * new String(r.data).toFloat)
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName requires to arguments of type NFNIntValue and not $args")
    }
  }
}
