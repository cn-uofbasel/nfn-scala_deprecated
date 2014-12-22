package nfn

import akka.actor._
import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}
import config.StaticConfig
import lambdacalculus.parser.ast._
import nfn.service.{WordCount, _}
import node.LocalNodeFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * This spec can be used for test which require a full rebuild of the topology.
 * This is useful for tests which should have a clean cache or for which the topology should be changed.
 * The base topology is based on the paper experiments.
 */
class PaperExperimentSpec extends FlatSpec with Matchers with ScalaFutures with SequentialNestedSuiteExecution {

  implicit val conf: Config = ConfigFactory.load()

  (1 to 6) map { expTest }
//  expTest(1)


  def expTest(n: Int) = {
    s"experiment $n" should "result in corresponding result in content object" in {
      doExp(n)
    }
  }
  def doExp(expNum: Int) = {

    val node1 = LocalNodeFactory.forId(1)
    val node2 = LocalNodeFactory.forId(2, isCCNOnly = true)

    val node3 = LocalNodeFactory.forId(3)

    val node4 = LocalNodeFactory.forId(4)
    val node5 = LocalNodeFactory.forId(5, isCCNOnly = true)
    val nodes = List(node1, node2, node3, node4, node5)

    val docdata1 = "one".getBytes
    val docname1 = node1.localPrefix.append("doc", "test1")
    val content1 = Content(docname1, docdata1)

    val docdata2 = "two two".getBytes
    val docname2 = node2.localPrefix.append("doc", "test2")
    val content2 = Content(docname2, docdata2)


    val docdata3 = "three three three".getBytes
    val docname3 = node3.localPrefix.append("doc", "test3")
    val content3 = Content(docname3, docdata3)

    val docdata4 = "four four four four".getBytes
    val docname4 = node4.localPrefix.append("doc", "test4")
    val content4 = Content(docname4, docdata4)

    val docdata5 = "five five five five five".getBytes
    val docname5 = node5.localPrefix.append("doc", "test5")
    val content5 = Content(docname5, docdata5)

    node1 <~> node2
    if (expNum != 3) {
      node1.registerPrefixToNodes(node2, List(node4))
      node2.registerPrefixToNodes(node1, List(node3))
    } else {
      node1.registerPrefixToNodes(node2, List(node3, node4, node5))
    }

    if (expNum != 3) {
      node1 <~> node3
      node1.registerPrefixToNodes(node3, List(node4))
    }

    node2 <~> node4
    node2.registerPrefixToNodes(node4, List(node3, node5))
    node4.registerPrefixToNodes(node2, List(node1))

    node3 <~> node4
    node3.registerPrefixToNodes(node4, List(node2))
    node4.registerPrefixToNodes(node3, List(node1))

    node3 <~> node5
    node5.registerPrefixToNodes(node3, List(node1))

    // remove for exp 6
    if (expNum != 6) {
      node4 <~> node5
      node5.registerPrefixToNodes(node4, List(node2))
    } else {
      node4.registerPrefixToNode(node5, node3)
    }
    node1 += content1
    node2 += content2
    node3 += content3
    node4 += content4
    node5 += content5

    // remove for exp6
    if (expNum != 6) {
      node3.publishService(new WordCount())
    }

    node4.publishService(new WordCount())

    val wcPrefix = new WordCount().ccnName

    // remove for exp3
    if (expNum != 3 && expNum != 7) {
      node1.registerPrefix(wcPrefix, node3)
    } else if (expNum != 7) {
      node1.registerPrefix(wcPrefix, node2)
    }

    node2.registerPrefix(wcPrefix, node4)
    if (expNum == 7) {
      node2.registerPrefix(wcPrefix, node1)
    }

    node5.registerPrefix(wcPrefix, node3)

    if (expNum != 6) {
      node5.registerPrefix(wcPrefix, node4)
    } else {

      node3.registerPrefix(wcPrefix, node4)
    }

    val dynServ = new NFNDynamicService {
      override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
        println("yay")
        NFNIntValue(42)
      }
    }
    node1.publishService(dynServ)

    Thread.sleep(1000)

    import nfn.LambdaNFNImplicits._
    implicit val useThunks: Boolean = false

    val ts = new Translate()
    val wc = new WordCount()
    val nack = new NackServ()

    val exp1 = wc call docname1

    val res1 = "1"

    val exp2 = wc call docname5
    val res2 = "5"

    // cut 1 <-> 3:
    // remove <~>
    // remove prefixes
    // add wc face to node 2
    // remove wc face to node 3
    val exp3 = wc call docname5
    val res3 = "5"

    // thunks vs no thunks
    val exp4 = (wc call docname3) + (wc call docname4)
    val res4 = "7"

    val exp5_1 = wc call docname3
    val res5_1 = "3"
    val exp5_2 = (wc call docname3) + (wc call docname4)
    val res5_2 = "4"


    // node 3 to ccn only (simluate "overloaded" router)
    // cut 4 <-> 5
    // wc face from 3 to 4
    val exp6 = wc call docname5
    val res6 = "5"

    // Adds the wordcountservice to node1 and adds routing from node2 to 1
    val exp7 = (wc call docname4) + (wc call docname3)
    val res7 = "7"

    val exp8 = nack.call
    val res8 = Nack(CCNName("")).content

    expNum match {
      case 1 => doExp(exp1, res1)
      case 2 => doExp(exp2, res2)
      case 3 => doExp(exp3, res3)
      case 4 => doExp(exp4, res4)
      case 5 => doExp(exp6, res6)
      case 6 => doExp(exp7, res7)
//      case 7 => doExp(exp8, res8)
//      case 9 => doExp(exp9)
      case _ => throw new Exception(s"expNum can only be 1 to 6 and not $expNum")
    }

    def doExp(exprToDo: Expr, res: String) = {
      val f: Future[Content] = node1 ? exprToDo
      f.foreach { _ =>
        nodes foreach { _.shutdown() }
      }

      implicit val patienceConfig = PatienceConfig(Span(StaticConfig.defaultTimeoutDuration.toMillis, Millis), Span(100, Millis))
      whenReady(f) { content =>
        new String(content.data) shouldBe (res)
      }
    }
//    Thread.sleep(StaticConfig.defaultTimeoutDuration.toMillis + 100)

  }
}

