package evaluation

import java.io.File

import ccn.packet.{NFNInterest, Interest, CCNName, Content}
import com.typesafe.config.{ConfigFactory, Config}
import lambdacalculus.parser.ast.Expr
import myutil.IOHelper
import nfn.service.{PandocTestDocuments, Pandoc}
import node.LocalNodeFactory
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

object PandocApp extends App {

  implicit val conf: Config = ConfigFactory.load()

  val node1 = LocalNodeFactory.forId(1)
//  val node2 = LocalNodeFactory.forId(2)
//  val nodes = List(node1, node2)
  val nodes = List(node1)
//  node1 <~> node2

  val pandocServ = new Pandoc()

  val pandoc = node1.localPrefix.append(pandocServ.ccnName).toString

  val tutorialContent = PandocTestDocuments.tutorialMd(node1.localPrefix)
  val tinyContent = PandocTestDocuments.tinyMd(node1.localPrefix)
  node1.publishServiceLocalPrefix(pandocServ)
  node1 += Content(tutorialContent.name, tutorialContent.data.take(20000))
  node1 += tinyContent





//  node2.addPrefixFace(pandocServ.ccnName, node1)

  Thread.sleep(1000)
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val exprTut: Expr = pandoc appl(tutorialContent.name, str("markdown_github"), str("html"))
  val exprTiny: Expr = pandoc appl(tinyContent.name, str("markdown_github"), str("html"))

//  sendAndPrintForName(Interest(CCNName("call 4 /nfn_service_impl_Pandoc /node/node1/doc/tutorial/tutorialmd 'markdown_github' 'latex'", "NFN")))
  sendAndPrintForName(exprTut)

  def sendAndPrintForName(interest: Interest) = {
    val startTime = System.currentTimeMillis
    node1 ? interest onComplete {
      case Success(resultContent) => {
        val runTime = System.currentTimeMillis - startTime
        println(s"RESULT:\n${new String(resultContent.data)}\nTIME: (${runTime}ms)")
        nodes foreach { _.shutdown() }
      }
      case Failure(e) => println(s"Could not receive content for $interest")
        nodes foreach { _.shutdown() }
    }

  }
}
