package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import scala.io.Source


class ListIntervalService() extends  NFNService {

  val filename =   "./availabledata.txt"

  override def pinned = true

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
     case Seq(start: NFNIntValue, end: NFNIntValue) => {
        val uStart = start.i
        val uEnd = end.i

        val list =  Source.fromFile(filename).getLines.toList.map(s => new CCNName(s.split("/").tail.toList, None))
        val point = list.filter(v => v.cmps.last.toLong <= uEnd && v.cmps.last.toLong >= uStart)

        val string : String = point.foldLeft("_") (_ + "\n" + _.toString)
        return NFNStringValue(string)
      }
    }
  }
}
