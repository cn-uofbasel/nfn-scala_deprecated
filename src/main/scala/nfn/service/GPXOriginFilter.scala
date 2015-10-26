package nfn.service

import akka.actor.ActorRef
import akka.util.Timeout
import ccn.packet.{Interest, Content, CCNName}
import nfn.NFNApi
import akka.pattern._

import scala.concurrent.{Await, Future}

/**
 * Created by blacksheeep on 22/10/15.
 */
class GPXOriginFilter extends NFNService {

  type GPXPoint = (Double, Double, String)

  def requestdata(name: NFNStringValue, num: Int, ccnApi: ActorRef): Content = {

    val nameInputdata = CCNName(name.str.substring(1).split("/").toList, None)
    val interestInputdata = Interest(nameInputdata.append("p" + num.toString))

    implicit val timeout = Timeout(10000)
    val fres1: Future[Content] = (ccnApi ? NFNApi.CCNSendReceive(interestInputdata, false)).mapTo[Content]
    val resInputdata = Await.result(fres1, timeout.duration)

    resInputdata
  }

  def parsedata(inputdata: Content) : GPXPoint = {

    val input = new String(inputdata.data.map(_.toChar));
    val xml = scala.xml.XML.loadString(input)

    val latitude = xml  \ "@lat"
    val longitude = xml \ "@lon"
    val time =  xml \ "time"

    (latitude.text.toDouble, longitude.text.toDouble, time.text)
  }

  def createXMLResult(p: GPXPoint): String = {
    <trkpt lat={p._1.toString} lon={p._2.toString}>
      <time>{p._3}</time>
    </trkpt>.toString()
  }

  def north_pole_filter(p1: GPXPoint, pn: GPXPoint): GPXPoint = {
    (pn._1 - p1._1, pn._2 - p1._2, pn._3)
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head) match{
      case (name: NFNStringValue, num: NFNIntValue) => {

        //Request the input data
        val inputdata = requestdata(name, num.i, ccnApi)
        val refdata = requestdata(name, 1, ccnApi)

        //Parse Data
        val p_req = parsedata(inputdata)
        val p_ref = parsedata(refdata)

        val filtered = north_pole_filter(p_ref, p_req)

        NFNStringValue(createXMLResult(filtered))
      }
      case _ => ???

    }
  }
}
