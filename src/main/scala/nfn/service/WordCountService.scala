package nfn.service

import akka.actor.ActorRef

class WordCountService() extends NFNService {

  override def function: (Seq[NFNValue], ActorRef) => NFNValue = { (docs, _) =>

    def splitString(s: String): Int = s.split(" ").size

    Thread.sleep(500)
    NFNIntValue(
      docs.map({
        case doc: NFNContentObjectValue => splitString(new String(doc.data))
        case NFNStringValue(s) => splitString(s)
        case NFNIntValue(i) => 1
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $docs")
      }).sum
    )
  }
}
