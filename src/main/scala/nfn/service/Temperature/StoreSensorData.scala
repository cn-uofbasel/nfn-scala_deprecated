package nfn.service.Temperature

import java.io.{FileWriter, File}

import akka.actor.ActorRef
import akka.util.Timeout
import ccn.packet.{NFNInterest, Interest, Content, CCNName}
import lambdacalculus.parser.ast.{Call, Constant, Str}
import nfn.NFNApi
import nfn.service._
import nfn.tools.Networking.fetchContent
import scala.concurrent.duration._
import nfn.tools.Helpers._

import scala.reflect.io.Path

/**
 * Created by blacksheeep on 16/11/15.
 */
class StoreSensorData extends NFNService   {



  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false


  def requestData(sensorname: String, sensortype: String, datapoint: Int, ccnApi: ActorRef) : Option[Content] = {

    val sname = CCNName(sensorname.split("/").toList, None)


    val expr: Call = sname call (Str(sensortype), Constant(datapoint))
    val interest = NFNInterest(expr)


    fetchContent(interest, ccnApi, 30 seconds)
  }

  def storeData(sensorname: String, sensortyp: String, datapoint: Int, data: String) : Unit = {
    val datastring = sensorname + ";" + sensortyp + ";" + datapoint.toString + ";" + data + "\n"

    writeToFile("database.txt", datastring)

  }

  def writeToFile(p: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new FileWriter(p, true))
    try pw.append(s) finally pw.close()
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    (args.head, args.tail.head, args.tail.tail.head) match {
      case (sensorname: NFNStringValue, sensortyp: NFNStringValue, datapoint: NFNIntValue) => {
        val contentOpt = requestData(sensorname.str, sensortyp.str, datapoint.i, ccnApi)

        contentOpt match{
          case Some(c) => {
            storeData(sensorname.str, sensortyp.str, datapoint.i, new String(c.data)) //TODO
            NFNStringValue(new String(c.data))
          }
          case _ => ???
        }
      }
      case _ => ???
    }

  }
}
