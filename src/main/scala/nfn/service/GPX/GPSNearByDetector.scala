package nfn.service.GPX

import akka.actor.ActorRef
import ccn.packet.{Content, Interest, CCNName, NFNInterest}
import nfn.service._
import nfn.tools.Networking._

import scala.concurrent.duration._
import nfn.service.GPX.helpers._
/**
 * Created by blacksheeep on 20/01/16.
 */
class GPSNearByDetector extends  NFNService {


  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(s1: NFNStringValue, s2: NFNStringValue, ix : NFNIntValue) => {
        val s1name = s1.str.split('/').toList
        val s2name = s2.str.split('/').toList
        val ixi = ix.i

        val pos: List[String] = List(s"p$ixi")
        val s1namePos: List[String] = s1name ++ pos

        val i1 = Interest(CCNName(s1namePos, None))
        val c1 = fetchContent(i1, ccnApi, 30 seconds).get
        val r1 = new String(c1.data.map(_.toChar))

        val (lat1, lon1, time1) = GPXPointHandler.parseGPXPoint(c1)
        val ts = parseTS(time1)
        val closestOther = findClosestDP(ccnApi, s2, ts, ixi)


        val pos2ndPoint: List[String] = List(s"p$closestOther")
        val s2namePos: List[String] = s2name ++ pos2ndPoint
        val i2 = Interest(CCNName(s2namePos, None))

        val c2 = fetchContent(i2, ccnApi, 30 seconds).get
        val r2 = new String(c2.data.map(_.toChar))
        val (lat2, lon2, time2) = GPXPointHandler.parseGPXPoint(c2)


        val dist = GPXPointHandler.computeDistance(lat1, lon1, lat2, lon2).get
        //NFNFloatValue(dist)

        if (dist < 0.015) NFNIntValue(1) else NFNIntValue(0)




      }
      case _ =>
        throw new NFNServiceArgumentException(s"Parameter Mismatch")
    }
  }

  def parseTS(data: String): Long = {
    val year = data.substring(0,4).toLong
    val month = data.substring(5,7).toLong
    val day = data.substring(8,10).toLong

    val hour = data.substring(11,13).toLong
    val minute = data.substring(14,16).toLong
    val second = data.substring(17,19).toLong

    val timestamp = 31536000L*(year-1970) + 2592000L*month + 86400L*day + 3600L*hour + 60L*minute + second

    return timestamp

  }

  def timeStampDistance(data1: Long, data2: Long) : Long = {
    return Math.abs(data1-data2)
  }

  def findClosestDP(ccnApi: ActorRef, stream: NFNStringValue, timestamp: Long, startpos : Int): Int = {
    val prefix: List[String] = stream.str.split('/').toList

    var ret = List[Int]()
    var sp = if(startpos - 25 > 2) startpos else 2
    var found = true
    var distance = Int.MaxValue
    var smallest = sp
    try {
      while (found) {
        val pos: List[String] = List(s"p$sp")
        val name: List[String] = prefix ++ pos
        val i = Interest(CCNName(name, None))
        val cOp = fetchContent(i, ccnApi, 10 seconds)

        cOp match {
          case Some(c) => {
            val s = new String(c.data.map(_.toChar))
            val point = GPXPointHandler.parseGPXPoint(c) //TODO parsing error!!!!
            val ts = parseTS(point._3)
            val d = timeStampDistance(ts, timestamp)
            if (d < distance) {
              smallest = sp
              distance = d.toInt
            }
            if (sp > (startpos + 25)) return smallest
            sp = sp + 1
          }
          case _ => {
            found = false
            return smallest
          }
          //compare Timestamp and compute distance
        }
      }
    }
    catch {
      case _ => return smallest
    }
    return smallest
  }

}

