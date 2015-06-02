package runnables.evaluation

import ccn.packet.Interest
import lambdacalculus.parser.ast.Expr

import com.typesafe.config.{Config, ConfigFactory}
import monitor.Monitor
import nfn.service.Http.{HttpPostQueryStringBuilderService, HttpGetQueryStringBuilderService, HttpService}
import nfn.service._
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * Created by rgasser on 26.01.15.
 */
object RestfulExperiment extends App {

  implicit val conf: Config = ConfigFactory.load()

  /* Prepare a single node (node 1) */
  val node1 = LocalNodeFactory.forId(1)
  val node2 = LocalNodeFactory.forId(2)

  val nodes = List(node1,node2)

  /* Connect the two nodes. */
  node1 <~> node2

  /* Fetch a file from the filesystem (adjust path to your needs) and register it as acontent object for the current node. */
  val exampleContent = FileSystemDocument.documentWithPath("/Users/rgasser/Desktop/test.pdf", node2.localPrefix)
  node2 += exampleContent

  /* Instantiate services... */
  val httpService = new HttpService()
  val urlBuilderService = new HttpGetQueryStringBuilderService()
  val postQueryBuilderService = new HttpPostQueryStringBuilderService()

  /* ... and register them at node 1. */
  val http = node1.localPrefix.append(httpService.ccnName)
  node1.publishServiceLocalPrefix(httpService)
  val url = node1.localPrefix.append(urlBuilderService.ccnName)
  node1.publishServiceLocalPrefix(urlBuilderService)
  val postQuery = node1.localPrefix.append(postQueryBuilderService.ccnName)
  node1.publishServiceLocalPrefix(postQueryBuilderService)

  Thread.sleep(1000)

  /* Now the experimenting begins. */
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  /** HTTP GET Experiment (Apache Solr API): Executes a query using a local Apache Solr instance. */
  val exprGetSolr: Expr = http call("GET", url call("https://tst.kgapi.bl.ch/solr/kim-portal.objects/query", "q", "*:*"))

  /** 1) HTTP GET Test (Dropbox API). */
  /** 1a) Fetch account information from Dropbox. */
  val exprGetAccount: Expr =  http call("GET", url call ("https://api.dropbox.com/1/account/info"), "--header", "Authorization", "Bearer <Access Token>")

  /** 1b) Fetch a simple file (content) from the Dropbox. */
  val exprGetMetadata: Expr = http call("GET", url call("https://api-content.dropbox.com/1/metadata/auto/test.txt"), "--header", "Authorization", "Bearer <Add token here>")

  /** 1c) Fetch a the metadata for a file from the Dropbox API. */
  val exprGetFile: Expr = http call("GET", url call("https://api-content.dropbox.com/1/files/auto/test.txt"), "--header", "Authorization", "Bearer <Add token here>")

  /** 2) HTTP PUT Test (Dropbox API). */
  /** 2a) Simple 'Hello World' example that safes a string ('Hello World') to the Dropbox. */
  val exprPutSimple : Expr = http call("PUT", url call("https://api-content.dropbox.com/1/files_put/auto/test/test.txt"), "--body", "Hello World", "--header", "Authorization", "Bearer <Add token here>")

  /** 2b) More advanced example that saves the previously specified content object to the Dropbox. */
  val exprPutFile : Expr = http call("PUT", url call("https://api-content.dropbox.com/1/files_put/auto/test.pdf"), "--body", exampleContent.name, "--header", "Authorization", "Bearer <Add token here>")

  /** 3) HTTP POST Test (Dropbox API). */
  /** 3a) Removes a specific document from your dropbox . */
  val exprPostDelete : Expr = http call("POST", url call("https://api.dropbox.com/1/fileops/delete"), "--body", postQuery call("root", "auto", "path", "test.txt"), "--header", "Authorization", "Bearer <Add token here>")

  /** Execute selected experiment. */
  sendAndPrintForName(exprGetSolr)

  /**
   *
   * @param interest
   */
  def sendAndPrintForName(interest: Interest) = {
    val startTime = System.currentTimeMillis
    println(s"Sending request: $interest")
    node1 ? interest onComplete {
      case Success(resultContent) => {
        val runTime = System.currentTimeMillis - startTime
        println(s"RESULT:\n${new String(resultContent.data)}\nTIME: (${runTime}ms)")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach { _.shutdown() }
      }
      case Failure(e) => println(s"Could not receive content for $interest")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach { _.shutdown() }
    }

  }
}
