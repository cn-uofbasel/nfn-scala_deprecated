package runnables.evaluation

import ccn.packet.{Content, CCNName}
import com.typesafe.config.{Config, ConfigFactory}
import lambdacalculus.parser.ast.{Constant, Expr, Str}
import monitor.Monitor
import nfn.LambdaNFNImplicits._
import nfn.service.Temperature._
import nfn.service._
import scala.io.Source
import sys.process._
import node.{LocalNode, LocalNodeFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object GPSEventNet extends App {
  implicit val conf: Config = ConfigFactory.load()
  implicit val useThunks: Boolean = false

  println("Starting network...")
  //=== setup nodes, node 4 and 5 are sensor nodes
  val node1 = LocalNodeFactory.forId(1)
  val node2 = LocalNodeFactory.forId(2)
  val node3 = LocalNodeFactory.forId(3)
  val node4 = LocalNodeFactory.forId(4, isCCNOnly = true) //contains GPS data
  val node5 = LocalNodeFactory.forId(5, isCCNOnly = true) //contains GPS data

  loadTrack("chris", node4)
  loadTrack("urs", node5)


  //== setup connections
  node1 <~> node2
  node2 <~> node4
  node1 <~> node3
  node3 <~> node5

  //add data to node 5


  node1.registerPrefix(node4.localPrefix, node2)
  node1.registerPrefix(node5.localPrefix, node3)

  val sensorService = new ReadSensorData

  node1.publishServiceLocalPrefix(new AddService)
  node1.publishServiceLocalPrefix(new DifferenceService)
  node1.publishServiceLocalPrefix(new MinusService)
  node1.publishServiceLocalPrefix(new MultService)
  node1.publishServiceLocalPrefix(new PredictionService)

  node1.publishServiceLocalPrefix(new GPSNearByDetector)

  println("+++++++Initializion finished+++++++")


  //TODO build interest


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

  def exit = {
    println("exit")
    Monitor.monitor ! Monitor.Visualize()
   // nodes foreach { _.shutdown() }
  }

  def loadTrack(name: String, node: LocalNode) :Unit = {
    val files =  ("ls /Users/blacksheeep/Desktop/GPX/" + name !!)
    var filelist = files.split('\n')

    filelist.foreach(f => {
      println(f)
      //val data = Source.fromFile(s"trackpoints/$f").mkString
      val num = f.substring(f.indexOf("_") + 1, f.indexOf("."))
      node += Content(CCNName(s"/ndn/ch/unibas/NDNfit/$name/gpx/data/p$num".substring(1).split("/").toList, None), data.getBytes)
    })
  }
}
