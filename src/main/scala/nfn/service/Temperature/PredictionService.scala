package nfn.service.Temperature

import akka.actor.ActorRef
import ccn.packet.{NFNInterest, CCNName}
import lambdacalculus.parser.ast.{Constant, Str}
import nfn.service._
import nfn.tools.Networking._
import scala.concurrent.duration._
/**
 * Created by blacksheeep on 17/11/15.
 */



class PredictionService  extends NFNService    {


  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head, args.tail.tail.head, args.tail.tail.tail.head, args.tail.tail.tail.tail.head) match {
      case (tempSensorDB: NFNStringValue, preasureSensorDB: NFNStringValue, tempSensor: NFNStringValue, preasureSensor: NFNStringValue, predictionNum: NFNIntValue) => {

          val tempSensorDBName = CCNName(tempSensorDB.str.substring(1).split('/').toList, None)
          val tempSensorName = CCNName(tempSensor.str.substring(1).split('/').toList, None)

          val preasureSensorDBName = CCNName(preasureSensorDB.str.substring(1).split('/').toList, None)
          val preasureSensorName = CCNName(preasureSensor.str.substring(1).split('/').toList, None)

          val q1 = tempSensorDBName call (Str(tempSensorName.toString.substring(1)), Str("Temperature"), Constant(predictionNum.i-1))
          val q2 = tempSensorDBName call (Str(tempSensorName.toString.substring(1)), Str("Temperature"), Constant(predictionNum.i-2))
          val q3 = preasureSensorDBName call (Str(preasureSensorName.toString.substring(1)), Str("Pressure"), Constant(predictionNum.i-2))
          val q4 = preasureSensorDBName call (Str(preasureSensorName.toString.substring(1)), Str("Pressure"), Constant(predictionNum.i-3))

          val q5 = tempSensorDBName call (Str(tempSensorName.toString.substring(1)), Str("Temperature"), Constant(predictionNum.i-3))



          val r1 = new String(fetchContent(NFNInterest(q1), ccnApi, 30 seconds).get.data).toInt
          val r2 = new String(fetchContent(NFNInterest(q2), ccnApi, 30 seconds).get.data).toInt
          val r3 = new String(fetchContent(NFNInterest(q3), ccnApi, 30 seconds).get.data).toInt
          val r4 = new String(fetchContent(NFNInterest(q4), ccnApi, 30 seconds).get.data).toInt


          val r5 = new String(fetchContent(NFNInterest(q5), ccnApi, 30 seconds).get.data).toInt

          val f = (r1.toFloat - r2.toFloat) / (r3.toFloat - r4.toFloat)


          NFNFloatValue(f * (r3.toFloat-r4.toFloat) + r5.toFloat)
      }
      case _ => ???
    }
  }
}
