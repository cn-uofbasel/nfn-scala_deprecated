package runnables.evaluation

import ccn.packet.Interest
import com.typesafe.config.{ConfigFactory, Config}
import lambdacalculus.parser.ast.{Str, Expr}
import nfn.service._
import node.LocalNodeFactory
import nfn.LambdaNFNImplicits._
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

object MapReduceApp extends App {
  implicit val conf: Config = ConfigFactory.load()
  val node1 = LocalNodeFactory.forId(1)

  val mapServ = new MapService()
  val map = node1.localPrefix.append(mapServ.ccnName)

  val reduceServ = new ReduceService()
  val reduce = node1.localPrefix.append(reduceServ.ccnName)

  val wcServ = new WordCount()
  val wc = node1.localPrefix.append(wcServ.ccnName)

  val sumServ = new SumService()
  val sum = node1.localPrefix.append(sumServ.ccnName)

  node1.publishServiceLocalPrefix(mapServ)
  node1.publishServiceLocalPrefix(reduceServ)
  node1.publishServiceLocalPrefix(wcServ)
  node1.publishServiceLocalPrefix(sumServ)

  val tutorialContent = PandocTestDocuments.tutorialMd(node1.localPrefix)
  val tinyContent = PandocTestDocuments.tinyMd(node1.localPrefix)
  node1 += tutorialContent
  node1 += tinyContent

  Thread.sleep(1000)

  implicit val useThunks: Boolean = false

  val mapExpr: Expr = map call (wc, /*Str("hello world"), Str("foobar"))*/ tutorialContent.name, tinyContent.name)

  val reduceExpr: Expr = reduce call (sum, mapExpr)

  println(s"Expression 1: $mapExpr")
  println(s"Expression 2: $reduceExpr")

  sendAndPrintForName(reduceExpr)

  def sendAndPrintForName(interest: Interest) = {
    val startTime = System.currentTimeMillis
    println(s"Sending request: $interest")
    node1 ? interest onComplete {
      case Success(resultContent) => {
        val runTime = System.currentTimeMillis - startTime
        println(s"RESULT:\n${new String(resultContent.data)}\nTIME: (${runTime}ms)")
        node1.shutdown()
        System.exit(0)
      }
      case Failure(e) => println(s"Could not receive content for $interest")
        node1.shutdown()
        System.exit(-1)
    }

  }
}
