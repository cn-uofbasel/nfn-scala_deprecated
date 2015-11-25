package runnables

import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}
import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._
import nfn.service.{PandocTestDocuments, WordCount}
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object TimeoutDebug extends App {

  implicit val conf: Config = ConfigFactory.load()
  implicit val useThunks: Boolean = false

  // -----------------------------------------------------------------------------
  // ==== NETWORK SETUP ==========================================================
  // -----------------------------------------------------------------------------


  /*


      +---+     +---+     +---+
      | a |-----| b |-----| c |
      +---+     +---+     +---+


   */

  // node setup
  val nodeA = LocalNodeFactory.forId(1, prefix=Option(CCNName("node", "a")))
  val nodeB = LocalNodeFactory.forId(2, prefix=Option(CCNName("node", "b")))
  val nodeC = LocalNodeFactory.forId(3, prefix=Option(CCNName("node", "c")))

  // connecting
  nodeA <~> nodeB
  nodeB <~> nodeC

  // routing
  nodeA.registerPrefixToNodes(nodeB, List(nodeC))
  nodeC.registerPrefixToNodes(nodeB, List(nodeA))

  // -----------------------------------------------------------------------------
  // ==== SERVICE AND CONTENT SETUP ==============================================
  // -----------------------------------------------------------------------------

  // install word count service on /node/C
  val wcServ = new WordCount()
  val wcName = nodeC.localPrefix.append(wcServ.ccnName)
  nodeC.publishServiceLocalPrefix(wcServ)
  println("Word Count Service: " + wcName)

  // publish document on nodeC
  val documentOnC = PandocTestDocuments.tinyMd(nodeC.localPrefix)
  val nameOnC = documentOnC.name
  nodeC += documentOnC
  println("Document on /node/c: " + nameOnC)

  // publish document on nodeA
  val documentOnA = PandocTestDocuments.tinyMd(nodeA.localPrefix)
  val nameOnA = documentOnA.name
  nodeA += documentOnA
  println("Document on /node/a: " + nameOnA)

  Thread.sleep(1000)

  // -----------------------------------------------------------------------------
  // ==== SEND INTERESTS =========================================================
  // -----------------------------------------------------------------------------

  def triggerWordCount(interest: Interest) = {
    val startTime = System.currentTimeMillis
    println("-----")
    println(s"Sending request: $interest")
    nodeA ? interest onComplete {
      case Success(resultContent) => {
        val runTime = System.currentTimeMillis - startTime
        println(s"RESULT:\n${new String(resultContent.data)}\nTIME: (${runTime}ms)")
      }
      case Failure(e) => println(s"Could not receive content for $interest")
    }

  }

  // prepare interests
  val exp1 = 'x @: ('x call nameOnC)
  val countC = Interest(exp1.name.prepend(wcName))

  val exp2 = 'x @: (wcName call 'x)
  val countA = Interest(exp2.name.prepend(nameOnA))

  // send
  triggerWordCount(countC)
  Thread.sleep(5000)
  triggerWordCount(countA) // this causes timeouts!

// -----------------------------------------------------------------------------
// ==== SHUTDOWN ===============================================================
// -----------------------------------------------------------------------------

Thread.sleep(20000)
println("-----")
println("Shutdown...")
nodeA.shutdown()
nodeB.shutdown()
nodeC.shutdown()
println("END")


}