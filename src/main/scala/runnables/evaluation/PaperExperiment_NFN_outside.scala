package runnables.evaluation

import ccn.packet.{CCNName, Interest, Content}
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

  //val node0 = LocalNodeFactory.forId(0) // node0 is a local nfn node


  val node11 = LocalNodeFactory.forId(1, port = 11)
  val node13 = LocalNodeFactory.forId(3, port = 13)
  val node14 = LocalNodeFactory.forId(4, port = 14)

  val node1 = LocalNodeFactory.forId(1, isCCNOnly = true, default_route_port = 10110)
  val node2 = LocalNodeFactory.forId(2, isCCNOnly = true)
  val node3 = LocalNodeFactory.forId(3, isCCNOnly = true, default_route_port = 10130)
  val node4 = LocalNodeFactory.forId(4, isCCNOnly = true, default_route_port = 10140)
  val node5 = LocalNodeFactory.forId(5, isCCNOnly = true)

  val nodes = List(node1, node2, node3, node4, node5, node11, node13, node14)

   //Direct connections
  node1 <~> node2
  node1 <~> node3
  node2 <~> node4
  node3 <~> node5
  node3 <~> node4
  node4 <~> node5

  //NFN connections
  node1 <~> node11
  node3 <~> node13
  node4 <~> node14

  //Add prefixes for multi hop connections
  node1.registerPrefixToNodes(node2, List(node4))
  node1.registerPrefixToNodes(node3, List(node4, node5))

  node2.registerPrefixToNodes(node4, List(node3, node5))

  node3.registerPrefixToNodes(node4, List(node2))

  node4.registerPrefixToNodes(node2, List(node1))
  node4.registerPrefixToNodes(node3, List(node1))

  node5.registerPrefixToNodes(node3, List(node1))

  node11.registerPrefixToNodes(node3, List(node1, node2, node4, node5))
  node13.registerPrefixToNodes(node3, List(node1, node2, node4, node5))

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

  //For cache test uncomment
  node3 += Content(docname4, docdata4)

  //install functions
  node11.publishServiceLocalPrefix(new WordCount())
  node13.publishServiceLocalPrefix(new WordCount())
  node14.publishServiceLocalPrefix(new WordCount())

  //add routing informations for the functions
  val wcPrefix1 = new WordCount().ccnName.prepend(node11.localPrefix)
  val wcPrefix3 = new WordCount().ccnName.prepend(node13.localPrefix)
  val wcPrefix4 = new WordCount().ccnName.prepend(node14.localPrefix)

  /*node1.registerPrefix(wcPrefix, node11)
  node3.registerPrefix(wcPrefix, node13)
  node4.registerPrefix(wcPrefix, node14)

  node2.registerPrefix(wcPrefix, node1)
  node2.registerPrefix(wcPrefix, node4)

  node5.registerPrefix(wcPrefix, node3)
  node5.registerPrefix(wcPrefix, node4)*/

  Thread.sleep(2000)

  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  //Test1
  val exp1 = 'x @: (wcPrefix1 call 'x)  // call 2 /../wc /../doc/test1 <-> /doc/test1 / @x call 2  /../wc / x
  val i1 = Interest(exp1.name.prepend(docname3))

  //Test2
  val exp2 = 'x @: ('x call docname5)
  val i2 = Interest(exp2.name.prepend(wcPrefix3))

  //Test3
  val exp3 = 'x @: (wcPrefix4 call 'x)
  val i3 = Interest(exp3.name.prepend(docname4))

  val exp4 = wcPrefix3 call docname3;
  val i4 = Interest(exp4)

  val i = i3
  //when sending an interest: no NFN component for exp4
  println(s"sending: ${i.name.cmps.mkString("[", " | ", "]")}")
  (node1 ? exp4).onComplete{
    case Success (c) => println(c)
    case Failure (e) => println(s"Error $e");
  }


}
