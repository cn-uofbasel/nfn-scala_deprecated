package nfn.service

import akka.actor.ActorRef

/**
 * Created by blacksheeep on 01.12.14.
 */
class Reverse extends NFNService{
  override def function: (Seq[NFNValue], ActorRef) => NFNValue = {
    (args, _) => {
      args match{
        case Seq(NFNStringValue(str)) => NFNStringValue(str.reverse)
        case _ => ???
      }


    }
  }
}
