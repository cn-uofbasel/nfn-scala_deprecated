package nfn.service.Temperature

import java.io.FileReader

import akka.actor.ActorRef
import nfn.service.{NFNIntValue, NFNStringValue, NFNValue, NFNService}

/**
 * Created by blacksheeep on 17/11/15.
 */



class AccessSensorData  extends NFNService    {

  def readData(sensorname: String, sensortyp: String, datapoint: Int) : String = {

    val lines = readFile("database.txt")
    val csl = lines.map(x => x.split(';'))
    val res = csl.find(x => x.head == sensorname && x.tail.head == sensortyp && x.tail.tail.head.toInt == datapoint)
    res match{
      case Some(s) => {
        s.tail.tail.tail.head
      }
      case _ => s"Error, $sensorname, $sensortyp, $datapoint"
    }
  }

  def readFile(p: String): List[String] = {
    scala.io.Source.fromFile(p).getLines().toList
  }

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head, args.tail.tail.head) match {
      case (sensorname: NFNStringValue, sensortyp: NFNStringValue, datapoint: NFNIntValue) => {
        val datavalue = readData(sensorname.str, sensortyp.str, datapoint.i)

        NFNStringValue(datavalue)
      }
      case _ => ???
    }
  }
}
