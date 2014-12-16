package network

import java.net.InetSocketAddress

import akka.actor._
import akka.event.Logging
import akka.io.{IO, Udp}
import akka.util.ByteString
import akka.actor.Stash
import scala.collection._

object UDPConnection {
  case class Send(data: Array[Byte])
  case class Received(data: Array[Byte], sendingRemote: InetSocketAddress)
  case class Handler(worker: ActorRef)

  val maxPacketSizeKB = 8*1024

  val UdpSocketOptions = List(
    Udp.SO.SendBufferSize(maxPacketSizeKB),
    Udp.SO.ReceiveBufferSize(maxPacketSizeKB)
   //,    Udp.SO.ReuseAddress(true)
  )

}

/**
 * A connection between a target socket and a remote socket.
 * Data is sent by sending a [[network.UDPConnection.Send]] message containing the data to be sent.
 * Received data is send to all registered workers. To register a worker send a [[UDPConnection.Handler]] message.
 * This actor initializes itself on preStart by sending a bind message to the IO manager.
 * On receiving a Bound message, it sets the ready method to its new [[akka.actor.Actor.Receive]] handler.
 * All Send messages received before being ready are queued up in a [[Stash]] and get unload (meaning send to self)
 * on connect.
 *
 * @param local Socket to listen for data
 * @param maybeTarget If Some(addr), the connection sends data to the target on receiving [[UDPConnection.Send]] messages
 */
class UDPConnection(val local:InetSocketAddress, val maybeTarget:Option[InetSocketAddress]) extends Actor with Stash {
  import context.system

  val name = self.path.name

  val logger = Logging(context.system, this)

  private val workers: mutable.ListBuffer[ActorRef] = mutable.ListBuffer()

  override def preStart() = {
    // IO is the manager of the akka IO layer, send it a request
    // to listen on a certain host on a port
    IO(Udp) ! Udp.Bind(self, local, UDPConnection.UdpSocketOptions)
  }

  def handleWorker(worker: ActorRef) = {
    logger.debug(s"Registered worker $worker")
    workers += worker
  }

  def receive: Receive = {
    // Received udp socket actor, change receive handler to ready method with reference to the socket actor
    case Udp.Bound(boundAddr) =>
      logger.info(s"$name ready")
      context.become(ready(sender))
      unstashAll()
    case Udp.CommandFailed(cmd) =>
      logger.error(s"Udp command '$cmd' failed")
    case send: UDPConnection.Send => {
      logger.debug(s"Adding to queue")
      stash()
    }
    case UDPConnection.Handler(worker) => handleWorker(worker)
  }

  def ready(socket: ActorRef): Receive = {
    case UDPConnection.Send(data) => {
      maybeTarget match {
        case Some(target) => {
          logger.debug(s"$name sending data to $target")
          if(data.size > UDPConnection.maxPacketSizeKB) {
            throw new Exception(s"The UDPSocket is only able to send packets with max size ${UDPConnection.maxPacketSizeKB} and not ${data.size}")
          } else {
            maybeTarget map { target =>
              socket ! Udp.Send(ByteString(data), target)
            }
          }
        }
        case None => logger.warning("Received Send message, but this socket was configured to be a receiver-only socket!")
      }
    }
    case Udp.Received(data, sendingRemote) => {
      forwardReceivedData(data, sendingRemote)
    }
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
    case UDPConnection.Handler(worker) => handleWorker(worker)
  }

  def forwardReceivedData(data: ByteString, sendingRemote: InetSocketAddress): Unit = {
    workers.foreach(_ ! UDPConnection.Received(data.toByteBuffer.array.clone(), sendingRemote))
  }
}

class UDPSender(remote: InetSocketAddress) extends Actor with Stash {
  import context.system

  val logger = Logging(context.system, this)

  override def preStart() = {
    IO(Udp) ! Udp.SimpleSender(UDPConnection.UdpSocketOptions)
  }


  override def receive: Actor.Receive = {
    case Udp.SimpleSenderReady => {
      logger.debug("ready")
      context.become(ready(sender))
      unstashAll()
    }
    case msg  => stash()
  }
  def ready(socket: ActorRef): Actor.Receive = {
    case UDPConnection.Send(data) =>
      if(data.size > UDPConnection.maxPacketSizeKB) {
        throw new Exception(s"The UDPSocket is only able to send packets with max size ${UDPConnection.maxPacketSizeKB} and not ${data.size}")
      } else {
        logger.debug(s"Sending data: '${new String(data)}")
        socket ! Udp.Send(ByteString(data), remote)
      }
  }
}

