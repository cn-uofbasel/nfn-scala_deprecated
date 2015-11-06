package nfn.service.GPX.helpers

import akka.actor.ActorRef
import nfn.service.GPX.helpers.GPXConfig._

import ccn.packet.{Interest, Content, CCNName}
import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._

import filterAccess.tools.Networking.fetchContent

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
object GPXInterestHandler {

  def buildRawGPXPointInterest(name: String, n: Int): Interest = {
    Interest(
      raw_prefix.append(CCNName(name.substring(1).split("/").toList, None)).append("p" + n.toString)
    )
  }

  def fetchRawGPXPoint(name:String, n:Int, ccnApi:ActorRef):Option[Content] = {
    val i = buildRawGPXPointInterest(name, n)
    fetchContent(i, ccnApi, 5 seconds)
  }

  def buildGPXDistanceComputerInterest(name1: String, n1: Int, name2: String, n2: Int): Interest =
    GPXDistanceComputerName call(name1, n1, name2, n2)

  def fetchGPXDistanceComputer(name1: String, n1: Int, name2: String, n2: Int, ccnApi:ActorRef): Option[Content] = {
    val i = buildGPXDistanceComputerInterest(name1, n1, name2, n2)
    fetchContent(i, ccnApi, 5 seconds)
  }

  def buildGPXDistanceAggregatorInterest(name: String, n: Int): Interest =
    GPXDistanceAggregatorName call(name, n)

  def fetchGPXDistanceAggregator(name:String, n:Int, ccnApi:ActorRef): Option[Content] = {
    val i = buildGPXDistanceAggregatorInterest(name, n)
    fetchContent(i, ccnApi, 5 seconds)
  }

}
