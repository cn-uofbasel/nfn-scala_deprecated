package evaluation

import ccn.packet.{Interest, CCNName, Content}
import com.typesafe.config.{ConfigFactory, Config}
import node.LocalNodeFactory
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

object LargeContentTest extends App {

  implicit val conf: Config = ConfigFactory.load()

  val expNum = 3


  val node1 = LocalNodeFactory.forId(1)
  val node2 = LocalNodeFactory.forId(2, isCCNOnly = true)
  val nodes = List(node1, node2)
  node1 <~> node2

  val contentNameSmall1 = node1.prefix.append(CCNName("content", "small"))
  val contentNameLarge1 = node1.prefix.append(CCNName("content", "large"))

  val smallData = ("1"*10).getBytes
  val largeData = ("1"*201).getBytes // atm segmentsize is 100

  val contentNameSmall2 = node2.prefix.append(CCNName("content", "small"))
  val contentNameLarge2 = node2.prefix.append(CCNName("content", "large"))

  val smallContent1 = Content(contentNameSmall1, smallData)
  val largeContent1 = Content(contentNameLarge1, largeData)
  val smallContent2 = Content(contentNameSmall2, smallData)
  val largeContent2 = Content(contentNameLarge2, largeData)


  node1 += smallContent1
  node1 += largeContent1
  node2 += smallContent2
  node2 += largeContent2


  Thread.sleep(100)
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  sendAndPrintForName(contentNameLarge1)

  def sendAndPrintForName(name: CCNName) = {
    node1 ? Interest(name)  onComplete {
      case Success(resultContent) => {
        println(s"RESULT: $resultContent")
        nodes foreach { _.shutdown() }
      }
      case Failure(e) => println(s"Could not receive content with name $name");
        nodes foreach { _.shutdown() }
    }

  }

}
