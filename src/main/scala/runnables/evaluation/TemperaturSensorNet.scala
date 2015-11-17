package runnables.evaluation

import ccn.packet.Interest
import com.typesafe.config.{ConfigFactory, Config}
import lambdacalculus.parser.ast.{Constant, Str, Expr}
import nfn.service
import nfn.service.Temperature.{AccessSensorData, StoreSensorData, ReadSensorData}
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

  val sensorService = new ReadSensorData

  node2.publishServiceLocalPrefix(new StoreSensorData)
  node2.publishServiceLocalPrefix(new AccessSensorData)

  node3.publishServiceLocalPrefix(new StoreSensorData)
  node3.publishServiceLocalPrefix(new AccessSensorData)

  node4.publishServiceLocalPrefix(new ReadSensorData)
  node5.publishServiceLocalPrefix(new ReadSensorData)

  Thread.sleep(20000)

  val storesensorservicename1 = node2.localPrefix.append((new StoreSensorData).name.substring(1))
  val accesssensordata1 = node2.localPrefix.append((new AccessSensorData).name.substring(1))

  val storesensorservicename2 = node3.localPrefix.append((new StoreSensorData).name.substring(1))
  val accesssensordata2 = node3.localPrefix.append((new AccessSensorData).name.substring(1))

  val sensorserviename1 = node4.localPrefix.append((new ReadSensorData).name.substring(1))
  val sensorserviename2 = node5.localPrefix.append((new ReadSensorData).name.substring(1))



  //val query1 = sensorserviename1 call (Str("Temperature"), Constant(0))

  val query2 = storesensorservicename1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(2))

  val query3 = accesssensordata1 call (Str(sensorserviename1.toString.substring(1)), Str("Temperature"), Constant(2))

  doExp(query3)


  def doExp(exprToDo: Expr) = {
    println(s"Running test: $exprToDo")
    val startTime = System.currentTimeMillis()
    node1 ? exprToDo andThen {
      case Success(content) => {
        val totalTime = System.currentTimeMillis - startTime
        println(s"RESULT($totalTime): $content")

      }
      case Failure(error) =>
        throw error

    }
  }


}
