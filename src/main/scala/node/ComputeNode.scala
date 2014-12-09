package node

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props, ActorRef}
import ccn.{CCNTLVWireFormat, CCNInterface}
import ccn.ccnlite.CCNLiteInterfaceCli
import nfn.{UDPConnectionWireFormatEncoder, UDPConnectionWireFormatEncoder$, ComputeServer}

case class ComputeNode() {
  val system = ActorSystem("ComputeServer")

  val wireFormat = CCNTLVWireFormat()

  val computeServer: ActorRef = system.actorOf(Props(classOf[ComputeServer]), name = "ComputeServer")

  val ccnIf:CCNInterface = CCNLiteInterfaceCli(wireFormat = wireFormat)

  val udpConnection: ActorRef = UDPConnectionWireFormatEncoder.apply(
    system,
    new InetSocketAddress(10011),
    new InetSocketAddress(10010),
    ccnIf
  )
}

