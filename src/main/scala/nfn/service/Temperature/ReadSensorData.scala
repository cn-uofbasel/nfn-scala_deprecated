package nfn.service.Temperature

import akka.actor.ActorRef
import nfn.service._
import sys.process._

/**
 * Created by blacksheeep on 21/01/16.
 */
class ReadSensorData() extends NFNService  {

  val consttemp = 20
  val constpreasure = 1000

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    (args.head) match { // sensorname, datapoint
      case (dataPoint: NFNIntValue) => {
          var data = ("cat /sys/bus/w1/devices/28-00043c6106ff/w1_slave" !!)

          NFNDataValue(data.getBytes())
      }
      case _ => ???
    }
  }
}
