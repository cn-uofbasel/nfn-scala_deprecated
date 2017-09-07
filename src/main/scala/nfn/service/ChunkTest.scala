package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName


class ChunkTest() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    var chunkCount = 3
    var chunkSize = 4096

//    args match {
//      case NFNIntValue(value) :: tail => chunkCount = value
//      case _ => throw NFNServiceArgumentException("Expected chunk count argument")
//    }
//    args match {
//      case NFNIntValue(value) :: tail => chunkSize = value
//      case _ => throw NFNServiceArgumentException("Expected chunk size argument")
//    }

    val totalLength = chunkSize * chunkCount
    val lineLength = 160
    val lineSep = "\n"
    val builder = new StringBuilder(totalLength)

    for (i <- 0 until chunkCount) {
      val header = s"[chunk #$i start]\n"
      val footer = s"\n[chunk #$i end]"
      val remainingLength = chunkSize - header.length - footer.length
      val newLineCount = Math.floor(remainingLength / (lineLength + lineSep.length)).toInt
      val chars = '-'.toString * (remainingLength - newLineCount)
      val text = chars.grouped(lineLength).mkString(lineSep)
      builder.append(header)
      builder.append(text)
      builder.append(footer)
    }

    NFNStringValue(builder.toString)


//    NFNIntValue(
//      args.map({
//        case doc: NFNContentObjectValue => splitString(new String(doc.data))
//        case NFNStringValue(s) => splitString(s)
//        case NFNIntValue(i) => 1
//        case _ =>
//          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
//      }).sum
//    )
  }
}

