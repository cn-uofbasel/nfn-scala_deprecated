package runnables.evaluation

import ccn.packet.{Interest, Content}
import com.typesafe.config.{ConfigFactory, Config}
import nfn.service.WordCount
import node.LocalNodeFactory
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

/**
 * Created by blacksheeep on 19.12.14.
 */
object PaperExperiment_NFN_outside extends App {
  implicit val conf: Config = ConfigFactory.load()

  val node1 = LocalNodeFactory.forId(1, isCCNOnly = true)
  val node2 = LocalNodeFactory.forId(2, isCCNOnly = true)
  val node3 = LocalNodeFactory.forId(3, isCCNOnly = true)
  val node4 = LocalNodeFactory.forId(4, isCCNOnly = true)
  val node5 = LocalNodeFactory.forId(5, isCCNOnly = true)

  val node11 = LocalNodeFactory.forId(1, port = 11)
  val node13 = LocalNodeFactory.forId(3, port = 13)
  val node14 = LocalNodeFactory.forId(4, port = 14)

  val nodes = List(node1, node2, node3, node4, node5, node11, node13, node14)

   //Direct connections
  node1 <~> node2
  node2 <~> node4
  node3 <~> node5
  node3 <~> node4
  node4 <~> node5

  //NFN connections
  node1 <~> node11
  node3 <~> node13
  node4 <~> node14

  //Add prefixes for multi hop connections
  node1.addNodeFaces(List(node4), node2)
  node1.addNodeFaces(List(node4, node5), node3)

  node2.addNodeFaces(List(node3, node5), node4)

  node3.addNodeFaces(List(node2), node4)

  node4.addNodeFaces(List(node1), node2)
  node4.addNodeFaces(List(node1), node3)

  node5.addNodeFaces(List(node1), node3)

  //Add some documents:
  val docname1 = node1.localPrefix.append("doc", "test1")
  val docdata1 = "one".getBytes

  val docname2 = node2.localPrefix.append("doc", "test2")
  val docdata2 = "two two".getBytes

  val docname3 = node3.localPrefix.append("doc", "test3")
  val docdata3 = "three three three".getBytes

  val docname4 = node4.localPrefix.append("doc", "test4")
  val docdata4 = "four four four four".getBytes

  val docname5 = node5.localPrefix.append("doc", "test5")
  val docdata5 = "five five five five five".getBytes

  //add documents, assume that compute nodes have a repository
  node11 += Content(docname1, docdata1)
  node2 += Content(docname2, docdata2)
  node13 += Content(docname3, docdata3)
  node14 += Content(docname4, docdata4)
  node5 += Content(docname5, docdata5)

  //install functions
  node11.publishServiceLocalPrefix(new WordCount())
  node13.publishServiceLocalPrefix(new WordCount())
  node14.publishServiceLocalPrefix(new WordCount())

  //add routing informations for the functions
  val wcPrefix = new WordCount().ccnName
  node1.addPrefixFace(wcPrefix, node11)
  node3.addPrefixFace(wcPrefix, node13)
  node4.addPrefixFace(wcPrefix, node14)

  node2.addPrefixFace(wcPrefix, node1)
  node2.addPrefixFace(wcPrefix, node4)

  node5.addPrefixFace(wcPrefix, node3)
  node5.addPrefixFace(wcPrefix, node4)

  Thread.sleep(5000)

  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val wc = wcPrefix.toString
  val exp1 = /*wc appl*/ (docname1) // call 2 /../wc /../doc/test1

  (node1 ? Interest(docname1)).onComplete{
    case Success (c) => println(c)
    case Failure (e) => println(s"Error $e");
  }


}
