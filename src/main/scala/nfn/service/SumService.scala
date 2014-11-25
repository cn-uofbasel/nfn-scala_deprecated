package nfn.service

import akka.actor.ActorRef

/**
 * Simple service which takes a sequence of [[NFNIntValue]] and sums them to a single [[NFNIntValue]]
 */
class SumService() extends NFNService {

  override def function: (Seq[NFNValue], ActorRef) => NFNValue = {
    (values: Seq[NFNValue], _) => {
      NFNIntValue(
        values.map({
          case i: NFNIntValue => i.i
          case _ => throw  new NFNServiceArgumentException(s"SumService requires a sequence of NFNIntValue's and not $values")
        }).sum
      )
    }
  }
}
