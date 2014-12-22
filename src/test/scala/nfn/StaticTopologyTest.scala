package nfn

import ccn.packet.Content
import com.typesafe.config.{Config, ConfigFactory}
import config.StaticConfig
import lambdacalculus.parser.ast.Expr
import nfn.service._
import node.LocalNodeFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfterAll, SequentialNestedSuiteExecution, Matchers, FlatSpec}

import scala.concurrent.Future

/**
 * This spec tests several expressions based on a static topology.
 * It is much faster than the [[PaperExperimentSpec]],
 * but it does not empty any caches between executed tests.
 * The topology is from the paper experiments.
 */
class StaticTopologyTest extends FlatSpec
                         with Matchers
                         with ScalaFutures
                         with SequentialNestedSuiteExecution
                         with BeforeAndAfterAll {
  implicit val conf: Config = ConfigFactory.load()


  //***************************************************************************
  // TOPOLOGY
  //***************************************************************************

  val node1 = LocalNodeFactory.forId(1)

  val node2 = LocalNodeFactory.forId(2, isCCNOnly = true)

  val node3 = LocalNodeFactory.forId(3)

  val node4 = LocalNodeFactory.forId(4)

  val node5 = LocalNodeFactory.forId(5, isCCNOnly = true)

  val nodes = List(node1, node2, node3, node4, node5)

  node1 <~> node2
  node1 <~> node3
  node1.registerPrefixToNodes(node2, List(node4))
  node1.registerPrefixToNodes(node3, List(node5))

  node2 <~> node4
  node2.registerPrefixToNodes(node4, List(node3, node5))

  node3 <~> node4
  node3 <~> node5
  node3.registerPrefixToNodes(node4, List(node2))

  node4 <~> node5
  node4.registerPrefixToNodes(node3, List(node1))
  node4.registerPrefixToNodes(node2, List(node1))

  node5.registerPrefixToNodes(node3, List(node1))
  node5.registerPrefixToNodes(node4, List(node2))

  //***************************************************************************
  // DOCUMENTS
  //***************************************************************************

  val docdata1 = "one".getBytes
  val docname1 = node1.localPrefix.append("doc", "test1")
  val content1 = Content(docname1, docdata1)
  node1 += content1

  val docdata2 = "two two".getBytes
  val docname2 = node2.localPrefix.append("doc", "test2")
  val content2 = Content(docname2, docdata2)
  node2 += content2


  val docdata3 = "three three three".getBytes
  val docname3 = node3.localPrefix.append("doc", "test3")
  val content3 = Content(docname3, docdata3)
  node3 += content3

  val tinyMdContent = PandocTestDocuments.tinyMd(node3.localPrefix)
  val tinyMdName = tinyMdContent.name
  val tinyMdExpected =
    """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      |<html xmlns="http://www.w3.org/1999/xhtml">
      |<head>
      |  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
      |  <meta http-equiv="Content-Style-Type" content="text/css" />
      |  <meta name="generator" content="pandoc" />
      |  <title></title>
      |  <style type="text/css">code{white-space: pre;}</style>
      |</head>
      |<body>
      |<h1 id="todo-list">TODO List</h1>
      |<ul>
      |<li><del>NOTHING</del></li>
      |</ul>
      |</body>
      |</html>""".stripMargin + "\n"
  node3 += tinyMdContent

  val tutorialMdContent = PandocTestDocuments.tutorialMd(node3.localPrefix)
  val tutorialMdName = tutorialMdContent.name
  node3 += tutorialMdContent

  val tutorialContent = PandocTestDocuments.tutorialMd(node3.localPrefix)
  val tutorialName = tutorialContent.name
  node3 += tutorialContent

  val docdata4 = "four four four four".getBytes
  val docname4 = node4.localPrefix.append("doc", "test4")
  val content4 = Content(docname4, docdata4)
  node4 += content4

  val docdata5 = "five five five five five".getBytes
  val docname5 = node5.localPrefix.append("doc", "test5")
  val content5 = Content(docname5, docdata5)
  node5 += content5


  //***************************************************************************
  // SERVICES
  //***************************************************************************

  val wcServ = new WordCount()
  val pandocServ = new Pandoc()
  val servs = List(wcServ, pandocServ)

  servs foreach { node3.publishService }
  servs foreach { node4.publishService }

  val wcPrefix = wcServ.ccnName
  val pandocPrefix = pandocServ.ccnName
  val prefixes = List(wcPrefix, pandocPrefix)

  prefixes foreach { node1.registerPrefix(_, node3) }
  prefixes foreach { node2.registerPrefix(_, node4) }
  prefixes foreach { node5.registerPrefix(_, node3) }
  prefixes foreach { node5.registerPrefix(_, node4) }

  Thread.sleep(1000)

  //***************************************************************************
  // TESTS
  //
  // Note that results of the tests are cached!
  //***************************************************************************


  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val wc = new WordCount()
  val pandoc = new Pandoc()


  testExpr(wc call docname1, "1")
  testExpr(wc call docname2, "2")
  testExpr(wc call docname3, "3")
  testExpr(wc call docname4, "4")
  testExpr(wc call docname5, "5")



  val tinyMd = pandoc call(tinyMdName, "markdown_github", "html")
  testExpr (
    tinyMd,
    tinyMdExpected
  )
  testExpr(
    wc call tinyMd,
    tinyMdExpected.split(" ").size.toString
  )

  val tutorialMd = pandoc call(tutorialMdName, "markdown_github", "html")
  testExpr(
    wc call tutorialMd ,
    "4410"
  )

  def testExpr(expr: Expr, res: String) = {
    s"expr: $expr" should s"result in $res" in {
      doExp(expr, res)
    }
  }

  def doExp(exprToDo: Expr, expected: String) = {
    val f: Future[Content] = node1 ? exprToDo
    implicit val patienceConfig = PatienceConfig(Span(StaticConfig.defaultTimeoutDuration.toMillis, Millis), Span(100, Millis))
    whenReady(f) { content =>
      val res = new String(content.data)
      res shouldBe expected
    }
  }

  override def afterAll(): Unit = {
    nodes foreach { _.shutdown() }
  }

}
