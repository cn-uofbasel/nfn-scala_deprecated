package nfn.service.GPS.GPX

import akka.actor.ActorRef
import ccn.packet.{Content, Interest, CCNName, NFNInterest}
import nfn.service._
import nfn.tools.Networking._

import scala.concurrent.duration._
import nfn.service.GPS.GPX.helpers._
/**
 * Created by blacksheeep on 20/01/16.
 */
class GPSNearByDetector extends  NFNService {


  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(s1: NFNStringValue, s2: NFNStringValue, refpoint : NFNIntValue, dist: NFNIntValue, maxval : NFNIntValue, firstStreamTimeOffset: NFNIntValue ,secondStreamTimeOffset: NFNIntValue) => {
        val s1name = s1.str.split('/').toList
        val s2name = s2.str.split('/').toList
        val ixi = refpoint.i

        val pos: List[String] = List(s"p$ixi")
        val s1namePos: List[String] = s1name ++ pos

        val i1 = Interest(CCNName(s1namePos, None))
        val c1 = fetchContent(i1, ccnApi, 30 seconds).get
        val r1 = new String(c1.data.map(_.toChar))

        val (lat1, lon1, time1) = GPXPointHandler.parseGPXPoint(c1)
        val ts = parseTS(time1)
        val (closestOther, timedist )= findClosestDP(ccnApi, s2, ts, ixi, maxval.i, firstStreamTimeOffset.i, secondStreamTimeOffset.i)


        val pos2ndPoint: List[String] = List(s"p$closestOther")
        val s2namePos: List[String] = s2name ++ pos2ndPoint
        val i2 = Interest(CCNName(s2namePos, None))

        val c2 = fetchContent(i2, ccnApi, 30 seconds).get
        val r2 = new String(c2.data.map(_.toChar))
        val (lat2, lon2, time2) = GPXPointHandler.parseGPXPoint(c2)


        val pointdist= GPXPointHandler.computeDistance(lat1, lon1, lat2, lon2).get
        //NFNFloatValue(dist)

        val distMeter = (pointdist*1000).toInt
        if (distMeter < dist.i) NFNStringValue(s"Point $ixi - close to: $closestOther - dist: $distMeter") else NFNStringValue(s"Point $ixi - Not Close enough to: $closestOther - dist: $distMeter - timedist: $timedist")




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

  def findClosestDP(ccnApi: ActorRef, stream: NFNStringValue, timestamp: Long, startpos : Int, maxval : Int, firstStreamTimeOffset : Int, secondStreamTimeOffset : Int): (Int, Int) = {


    val offset = 10
    val prefix: List[String] = stream.str.split('/').toList

    var ret = List[Int]()
    var sp = if(startpos - offset > 1) startpos - offset else 1
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
            val point = GPXPointHandler.parseGPXPoint(c)
            val ts = parseTS(point._3)
            val d = timeStampDistance(ts+secondStreamTimeOffset, timestamp+firstStreamTimeOffset)
            if (d < distance) {
              smallest = sp
              distance = d.toInt
            }
            if ((sp > (startpos + offset)) || (sp > maxval)) return (smallest, distance)
            sp = sp + 1
          }
          case _ => {
            found = false
            return (smallest, distance)
          }
          //compare Timestamp and compute distance
        }
      }
    }
    catch {
      case _ => return (smallest, distance)
    }
    return (smallest, distance)
  }

}

