package evaluation

import java.io.File

import ccn.packet.{NFNInterest, Interest, CCNName, Content}
import com.typesafe.config.{ConfigFactory, Config}
import lambdacalculus.parser.ast.Expr
import myutil.IOHelper
import nfn.service.impl.Pandoc
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

  val ccnlTutorialMdPath = "doc/tutorial/tutorial.md"

  val tutorialMdName = node1.prefix.append(CCNName(ccnlTutorialMdPath.split("/").toList.map{ n => n.replace(".", "")}, None))
  val ccnlHome = System.getenv("CCNL_HOME")
  val tutorialMdFile = new File(s"$ccnlHome/$ccnlTutorialMdPath")
  val tutorialMdData = IOHelper.readByteArrayFromFile(tutorialMdFile)
  val tutorialMdContent = Content(tutorialMdName, tutorialMdData)

  val pandocServ = new Pandoc()
  val pandoc = pandocServ.ccnName.toString
  node1.publishService(pandocServ)
  node1 += tutorialMdContent

//  node2.addPrefixFace(pandocServ.ccnName, node1)

  Thread.sleep(1000)
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val expr: Expr = pandoc appl(tutorialMdName, str("markdown_github"), str("latex"))

  sendAndPrintForName(Interest(CCNName("call 4 /nfn_service_impl_Pandoc /node/node1/doc/tutorial/tutorialmd 'markdown_github' 'latex'", "NFN")))
//  sendAndPrintForName(expr)

  def sendAndPrintForName(interest: Interest) = {
    node1 ? interest onComplete {
      case Success(resultContent) => {
        println(s"RESULT: $resultContent")
        nodes foreach { _.shutdown() }
      }
      case Failure(e) => println(s"Could not receive content for $interest")
        nodes foreach { _.shutdown() }
    }

  }
}
