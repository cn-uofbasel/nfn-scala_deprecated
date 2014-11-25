package nfn.service

import akka.actor.ActorRef

/**
 * Created by basil on 17/06/14.
 */
class NackServ extends NFNService {
  override def function: (Seq[NFNValue], ActorRef) => NFNValue = {
    throw new NFNServiceExecutionException("Provoking a nack")
  }
}