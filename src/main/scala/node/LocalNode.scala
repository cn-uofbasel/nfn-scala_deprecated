package node

import java.util.concurrent.TimeUnit

import ccn.ccnlite.CCNLiteInterfaceCli
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging
import nfn._
import scala.concurrent.duration.FiniteDuration
import akka.util.Timeout
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import config.{ComputeNodeConfig, RouterConfig, StaticConfig, AkkaConfig}
import ccn.packet._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import nfn.service.{NFNService, NFNServiceLibrary}
import scala.collection.immutable.{Iterable, IndexedSeq}
import ccn.CCNLiteProcess
import monitor.Monitor
import ccn.CCNLiteProcess

object LocalNodeFactory {

  def defaultMgmtSockNameForPrefix(prefix: CCNName, port: Option[Int] = None): String = {
    s"/tmp/mgmt.${prefix.cmps.mkString(".")}.${port.getOrElse("")}.sock"
  }

  /*
   * Claudio Marxer (April 15, 2015):
   *  Added optional parameter "prefix" to determine node prefix
   *  (Old version: See below.)
   */
  def forId(id: Int, isCCNOnly: Boolean = false, port: Int = 0, default_route_port: Int = 0, prefix:Option[CCNName]=None)(implicit config: Config): LocalNode = {

    val p = if(port == 0) id else port;
    val def_route_port = if(default_route_port != 0)"127.0.0.1/"+default_route_port else ""
    val nodePrefix = prefix match {
      case None => CCNName("node", s"node$id")
      case Some(n) => n
    }
    LocalNode(
      RouterConfig("127.0.0.1", 10000 + p * 10, nodePrefix, defaultMgmtSockNameForPrefix(nodePrefix, Some(p)), isCCNOnly = isCCNOnly, isAlreadyRunning = false, defaultNFNRoute = def_route_port),
      Some(ComputeNodeConfig("127.0.0.1", 10000 + p * 10 + 1, nodePrefix, withLocalAM = false))
    )
  }
}

/* OLD CODE:
  def forId(id: Int, isCCNOnly: Boolean = false, port: Int = 0, default_route_port: Int = 0)(implicit config: Config): LocalNode = {

    val p = if(port == 0) id else port;
    val def_route_port = if(default_route_port != 0)"127.0.0.1/"+default_route_port else ""
    val nodePrefix = CCNName("node", s"node$id")
    LocalNode(
      RouterConfig("127.0.0.1", 10000 + p * 10, nodePrefix, defaultMgmtSockNameForPrefix(nodePrefix, Some(p)), isCCNOnly = isCCNOnly, isAlreadyRunning = false, defaultNFNRoute = def_route_port),
      Some(ComputeNodeConfig("127.0.0.1", 10000 + p * 10 + 1, nodePrefix, withLocalAM = false))
    )
  }
}
 */

object LocalNode {
  /**
   * Connects the given sequence to a grid.
   * The size of the sequence does not have to be a power of a natural number.
   * Example:
   * o-o-o    o-o-o
   * | | |    | | |
   * o-o-o or o-o-o
   * | | |    | |
   * o-o-o    o-o
   * @param nodes
   */
  def connectGrid(nodes: Seq[LocalNode]): Unit = {
    if(nodes.size <= 1) return

    import Math._
    val N = nodes.size
    val roundedN = sqrt(N).toInt
    val n = roundedN + (if(N / roundedN > 0) 1 else 0)

    val horizontalLineNodes = nodes.grouped(n)
    horizontalLineNodes foreach { connectLine }

    // 0 1 2    0 3 6
    // 3 4 5 -> 1 4 7
    // 6 7 8    2 5 8
    val reshuffledNodes = 0 until N map { i =>
      val index = (i/n + n * (i % n)) % N
      println(index)
      nodes(index)
    }

    val verticalLineNodes = reshuffledNodes.grouped(n)
    verticalLineNodes foreach { connectLine }
  }

  /**
   * Connects head with every node in tail, this results in a star shape.
   * Example:
   *   o
   *   |
   * o-o-o
   * @param nodes
   */
  def connectStar(nodes: Seq[LocalNode]): Unit = {
    if(nodes.size <= 1) return

    nodes.tail foreach { _ <~> nodes.head }
  }

  /**
   * Connects every node with every other node (fully connected).
   * o - o
   * | X |
   * o - o
   * @param unconnectedNodes
   */
  def connectAll(unconnectedNodes: Seq[LocalNode]): Unit = {
    if(unconnectedNodes.size <= 1 ) return

    val head = unconnectedNodes.head
    val tail = unconnectedNodes.tail

    tail foreach { _ <~> head }
    connectAll(tail)
  }

  /**
   * Connects the nodes along the given sequence.
   * Example:
   * o-o-o-o
   * @param nodes
   * @return
   */
  def connectLine(nodes: Seq[LocalNode]): Unit = {
    if(nodes.size <= 1) return

    nodes.tail.foldLeft(nodes.head) {
      (left, right) => left <~> right; right
    }
  }
}

case class LocalNode(routerConfig: RouterConfig, maybeComputeNodeConfig: Option[ComputeNodeConfig]) extends Logging {

  implicit val timeout = StaticConfig.defaultTimeout

  val localPrefix = routerConfig.prefix

  val (maybeNFNServer: Option[ActorRef], maybeEc: Option[ExecutionContext]) =
    maybeComputeNodeConfig match {
      case Some(computeNodeConfig) =>
        val system = ActorSystem(s"Sys${computeNodeConfig.prefix.toString.replace("/", "-")}", AkkaConfig.config(StaticConfig.debugLevel))

        (Some(NFNServerFactory.nfnServer(system, routerConfig, computeNodeConfig)), Some(system.dispatcher))
      case None => (None, None)
    }

  implicit val ec = maybeEc.getOrElse(scala.concurrent.ExecutionContext.Implicits.global)

  val ccnLiteProcess: CCNLiteProcess = {
    val ccnLiteNFNNetworkProcess: CCNLiteProcess = CCNLiteProcess(routerConfig)
    ccnLiteNFNNetworkProcess.start()
    Thread.sleep(5)

    // If there is also a compute server, setup the local face
    maybeComputeNodeConfig map {computeNodeConfig =>
      if(!routerConfig.isCCNOnly) {
        ccnLiteNFNNetworkProcess.addPrefix(CCNName("COMPUTE"), computeNodeConfig.host, computeNodeConfig.port)
        //          ccnLiteNFNNetworkProcess.addPrefix(computeNodeConfig.prefix, computeNodeConfig.host, computeNodeConfig.port)
        Monitor.monitor ! Monitor.ConnectLog(computeNodeConfig.toNodeLog, routerConfig.toNodeLog)
        Monitor.monitor ! Monitor.ConnectLog(routerConfig.toNodeLog, computeNodeConfig.toNodeLog)
      }
    }
    ccnLiteNFNNetworkProcess
  }



  private def nfnMaster = {
    maybeNFNServer.get
  }

  /**
   * Sets up ccn-lite face to another node.
   * A ccn-lite face actually consists of two faces:
   * - a network face (currently UDP)
   * - and registration of the prefix und the face (there could be several prefixes under the same network face, but currently only one is supported)
   * @param otherNode
   */
  def connect(otherNode: LocalNode) = {
    Monitor.monitor ! Monitor.ConnectLog(routerConfig.toNodeLog, otherNode.routerConfig.toNodeLog)
    ccnLiteProcess.connect(otherNode.routerConfig)
  }

  def registerPrefixToNode(faceOfNode: LocalNode, gateway: LocalNode) = {
    registerPrefix(faceOfNode.routerConfig.prefix, gateway)
  }

  def registerPrefix(prefix: CCNName, gateway: LocalNode) = {
    val gatewayConfig = gateway.routerConfig
    ccnLiteProcess.addPrefix(prefix, gatewayConfig.host, gatewayConfig.port)
  }

  def registerPrefixToNodes(gateway: LocalNode, nodes: List[LocalNode]) = {
    nodes foreach { registerPrefixToNode(_, gateway) }
  }

  /**
   * Connects this with other and other with this, see [[connect]] for details of the connection process.
   * @param otherNode
   */
  def connectBidirectional(otherNode: LocalNode) = {
    this.connect(otherNode)
    otherNode.connect(this)
  }

  /**
   * Connects otherNode with this, see [[connect]] for details of the connection process
   * @param otherNode
   */
  def connectFromOther(otherNode: LocalNode) = otherNode.connect(this)

  /**
   * Symbolic method for [[connectBidirectional]]
   * @param otherNode
   */
  def <~>(otherNode: LocalNode) =  connectBidirectional(otherNode)

  /**
   * Symbolic method for [[connect]]
   * @param otherNode
   */
  def ~>(otherNode: LocalNode) = connect(otherNode)

  /**
   * Symbolic methdo for [[connectFromOther]]
   * @param otherNode
   */
  def <~(otherNode: LocalNode) = connectFromOther(otherNode)


  /**
   * Caches the given content in the node.
   * @param content
   */
  def cache(content: Content) =  nfnMaster ! NFNApi.AddToCCNCache(content)

  /**
   * Symoblic methdo for [[cache]]
   * @param content
   */
  def +=(content: Content) = cache(content)


  /**
   * Publishes the service with its name appended to the local prefix
   * @param serv
   */
  def publishServiceLocalPrefix(serv: NFNService) = {
    NFNServiceLibrary.nfnPublishService(serv, Some(localPrefix), nfnMaster)
  }
  
  def publishServiceCustomPrefix(serv: NFNService, prefix: CCNName): Unit = {
    NFNServiceLibrary.nfnPublishService(serv, Some(prefix), nfnMaster)
  }

  /**
   * Publishes the service with its name without modifying the name
   * @param serv
   */
  def publishService(serv: NFNService) = {
    NFNServiceLibrary.nfnPublishService(serv, None, nfnMaster)
  }

  /**
   * Fire and forgets an interest to the system. Response will still arrive in the localAbstractMachine cache, but will discarded when arriving
   * @param req
   */
  def send(req: Interest)(implicit useThunks: Boolean) = nfnMaster ! NFNApi.CCNSendReceive(req, useThunks)

  /**
   * Sends the request and returns the future of the content
   * @param req
   * @return
   */
  def sendReceive(req: Interest)(implicit useThunks: Boolean): Future[Content] = {
    (nfnMaster ? NFNApi.CCNSendReceive(req, useThunks)).mapTo[CCNPacket] map {
      case n: Nack => throw new Exception(":NACK")
      case c: Content =>  c
      case i: Interest => throw new Exception("An interest was returned, this should never happen")
      case a: AddToCacheAck => ???
      case a: AddToCacheNack => throw new Exception("Add content to cache failed!")
    }
  }


  /**
   * Symbolic method for [[send]]
   * @param req
   */
  def !(req: Interest)(implicit useThunks: Boolean) = send(req)

  /**
   * Symbolic method for [[sendReceive]]
   * @param req
   * @return
   */
  def ?(req: Interest)(implicit useThunks: Boolean): Future[Content] = sendReceive(req)

  /**
   * Shuts this node down. After shutting down, any method call will result in an exception
   */
  def shutdown() = {
    nfnMaster ! NFNServer.Exit()
    ccnLiteProcess.stop()
  }

}
