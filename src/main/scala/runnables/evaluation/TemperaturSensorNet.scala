package runnables.evaluation

import ccn.packet.Interest
import com.typesafe.config.{ConfigFactory, Config}
import lambdacalculus.parser.ast.{Constant, Str, Expr}
import nfn.service
import nfn.service.Temperature._
import nfn.service._
import node.LocalNodeFactory
import nfn.LambdaNFNImplicits._
import java.io.File

import ccn.packet.{Content, Interest}
import com.typesafe.config.{ConfigFactory, Config}
import monitor.Monitor
import nfn.service.{WordCount, PandocTestDocuments, Pandoc}
import node.LocalNodeFactory
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

/**
 * Created by blacksheeep on 13/11/15.
 */
object TemperaturSensorNet extends App {
  implicit val conf: Config = ConfigFactory.load()
  implicit val useThunks: Boolean = false

  println("Starting network...")
  //=== setup nodes, node 4 and 5 are sensor nodes
  val node1 = LocalNodeFactory.forId(1)
  val node2 = LocalNodeFactory.forId(2)
  val node3 = LocalNodeFactory.forId(3)
  val node4 = LocalNodeFactory.forId(4)
  val node5 = LocalNodeFactory.forId(5)

  //== setup connections
  node1 <~> node2
  node2 <~> node4
  node1 <~> node3
  node3 <~> node5


  node1.registerPrefix(node4.localPrefix, node2)
  node1.registerPrefix(node5.localPrefix, node3)

  val sensorService = new ReadSensorData

  node1.publishServiceLocalPrefix(new AddService)
  node1.publishServiceLocalPrefix(new DifferenceService)
  node1.publishServiceLocalPrefix(new MinusService)
  node1.publishServiceLocalPrefix(new MultService)
  node1.publishServiceLocalPrefix(new PredictionService)



  node2.publishServiceLocalPrefix(new StoreSensorData)
  node2.publishServiceLocalPrefix(new AccessSensorData)

  node3.publishServiceLocalPrefix(new StoreSensorData)
  node3.publishServiceLocalPrefix(new AccessSensorData)

  node4.publishServiceLocalPrefix(new ReadSensorData)
  node5.publishServiceLocalPrefix(new ReadSensorData)

  Thread.sleep(20000)

  val addname = node1.localPrefix.append((new AddService).name.substring(1))
  val difname = node1.localPrefix.append((new DifferenceService).name.substring(1))
  val minusname = node1.localPrefix.append((new MinusService).name.substring(1))
  val multname = node1.localPrefix.append((new MultService).name.substring(1))
  val predictionname = node1.localPrefix.append((new PredictionService).name.substring(1))

  val storesensorservicename1 = node2.localPrefix.append((new StoreSensorData).name.substring(1))
  val accesssensordata1 = node2.localPrefix.append((new AccessSensorData).name.substring(1))

  val storesensorservicename2 = node3.localPrefix.append((new StoreSensorData).name.substring(1))
  val accesssensordata2 = node3.localPrefix.append((new AccessSensorData).name.substring(1))

  val sensorserviename1 = node4.localPrefix.append((new ReadSensorData).name.substring(1))
  val sensorserviename2 = node5.localPrefix.append((new ReadSensorData).name.substring(1))


  val query01 = sensorserviename1 call (Str("Temperature"), Constant(0))
  val query02 = sensorserviename2 call (Str("Temperature"), Constant(2))

  val query11 = storesensorservicename1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(2))
  val query12 = storesensorservicename2 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(1))

  val query1 = accesssensordata1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(2))

  val query2 = accesssensordata2 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(2))

  val query3 = addname call (query1, query2)



  /*for (j <- 0 to 10) {

    val query11 = storesensorservicename1 call(Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(j))
    val query12 = storesensorservicename2 call(Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(j))
    doExp(query11)
    doExp(query12)
  }*/


  //val queryF = createQueryComputeF(0,1,2)
  //println(queryF)

  //doExp(queryF)

  val queryP = createQueryPredictNextValue(0,1,2)

  val queryPrediction = predictionname call (Str(accesssensordata1.toString), Str(accesssensordata2.toString), Str(sensorserviename1.toString), Str(sensorserviename2.toString), Constant(4))

  doExp(queryPrediction)


  def doExp(exprToDo: Expr) = {
    println(s"Running test: $exprToDo")
    val startTime = System.currentTimeMillis()
    node1 ? exprToDo andThen {
      case Success(content) => {
        val totalTime = System.currentTimeMillis - startTime
        println(s"RESULT($totalTime): $content")
        exit

      }
      case Failure(error) =>
        throw error

    }
  }

  def createQueryComputeF(t0: Int, t1: Int, t2: Int) : Expr = {
    val q1 = accesssensordata1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(t2))
    val q2 = accesssensordata1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(t1))

    val q3 = accesssensordata1 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(t1))
    val q4 = accesssensordata1 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(t0))

    difname call (minusname call (q1, q2), (minusname call (q3, q4)))
  }

  def createQueryPredictNextValue(t0: Int, t1: Int, t2: Int) : Expr = {

    val computeF = createQueryComputeF(t0,t1,t2)

    val getP2 = accesssensordata2 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(t2))

    val getP1 = accesssensordata2 call (Str(sensorserviename2.toString.substring(1)), Str("Pressure"), Constant(t1))

    val getT1 = accesssensordata1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(t1))

    val computeeNextValue = multname call (computeF, minusname call (getP2, getP1))


    computeeNextValue
    //addname call (computeeNextValue, getT1)
  }

  def exit = {
    println("exit")
    Monitor.monitor ! Monitor.Visualize()
   // nodes foreach { _.shutdown() }
  }


}
