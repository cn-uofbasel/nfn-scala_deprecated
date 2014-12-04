package nfn.service

import akka.actor.ActorRef

class WordCount() extends NFNService {
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    def splitString(s: String) = s.split(" ").size

    NFNIntValue(
      args.map({
        case doc: NFNContentObjectValue => splitString(new String(doc.data))
        case NFNStringValue(s) => splitString(s)
        case NFNIntValue(i) => 1
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }).sum
    )
  }
}

