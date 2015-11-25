package nfn.service.Temperature

import akka.actor.ActorRef
import nfn.service.{NFNStringValue, NFNIntValue, NFNValue, NFNService}
import scala.concurrent.duration._

/**
 * Created by blacksheeep on 13/11/15.
 */
class ReadSensorData() extends NFNService  {

  val consttemp = 20
  val constpreasure = 1000

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    (args.head, args.tail.head) match { // sensorname, datapoint
      case (sensorname: NFNStringValue, datapoint: NFNIntValue) => {

        sensorname.str match {
          case "Temperature" => {
            NFNIntValue(
              consttemp + (if (datapoint.i % 2 == 0) datapoint.i else (-datapoint.i))
            )
          }
          case "Pressure" => {
            NFNIntValue(
              constpreasure + (if (datapoint.i % 2 == 0) datapoint.i else (-datapoint.i))
            )
          }
        }
      }
      case _ => ???
    }
  }
}
