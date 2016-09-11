package testservice

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNIntValue, NFNService, NFNServiceArgumentException, NFNValue}


class OtherClass() {
  def foo(n: Int) = {
    println(s"OtherClass: $n")
  }
}

class ChfToDollar() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    println("External service successfully loaded!")
    new OtherClass().foo(3)
    if(args.size == 1 && args.head.isInstanceOf[NFNIntValue]) {
      NFNIntValue(args.head.asInstanceOf[NFNIntValue].i * 2)
    }
    else throw  new NFNServiceArgumentException(s"$ccnName can only be applied a single value of type NFNIntValue and not $args")
  }
}

object TestService extends App {
  println(new ChfToDollar())
}

