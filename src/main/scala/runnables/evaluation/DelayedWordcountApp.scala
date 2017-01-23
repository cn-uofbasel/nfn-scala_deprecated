package runnables.evaluation

import ccn.packet.Interest
import com.typesafe.config.{Config, ConfigFactory}
import lambdacalculus.parser.ast.Expr
import nfn.LambdaNFNImplicits._
import nfn.service.{Pandoc, PandocTestDocuments, DelayedWordCount}
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by blacksheeep on 22/07/15.
  */
object DelayedWordcountApp extends App{
  implicit val conf: Config = ConfigFactory.load()

  val node1 = LocalNodeFactory.forId(1)
  //  val node2 = LocalNodeFactory.forId(2)
  //  val nodes = List(node1, node2)
  //  val nodes = List(node1)
  //  node1 <~> node2

  val pandocServ = new Pandoc()
  val wcServ = new DelayedWordCount()

  val pandoc = node1.localPrefix.append(pandocServ.ccnName)
  val wc = node1.localPrefix.append(wcServ.ccnName)

  val tutorialContent = PandocTestDocuments.tutorialMd(node1.localPrefix)
  val tinyContent = PandocTestDocuments.tinyMd(node1.localPrefix)
  node1.publishServiceLocalPrefix(pandocServ)
  node1.publishServiceLocalPrefix(wcServ)
  node1 += tutorialContent
  node1 += tinyContent

  //  node2.addPrefixFace(pandocServ.ccnName, node1)

  Thread.sleep(1000)

  implicit val useThunks: Boolean = false

  val exprWc: Expr = wc call (tutorialContent.name)

  sendAndPrintForName(exprWc)

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
