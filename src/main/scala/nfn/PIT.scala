package nfn

import ccn.packet.CCNName
import akka.actor.{ActorContext, ActorRef}
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.mutable
import scala.concurrent.duration._

//case class PendingInterest(name: CCNName, faces: List[ActorRef], timeout: Long) extends Ordered[PendingInterest] {
//
//  val startTime = System.nanoTime
//
//  def timer = startTime + (timeout * 1000000)
//
//  override def compare(that: PendingInterest): Int = (this.timer - that.timer).toInt
//}

case class PIT(context: ActorContext) extends Logging {

  private case class Timeout(name: CCNName, face: ActorRef)

  private val pit = mutable.Map[CCNName, Set[ActorRef]]()

  def add(name: CCNName, face: ActorRef, timeout: FiniteDuration) = {
    pit += name -> (pit.getOrElse(name, Set()) + face)
  }

  def get(name: CCNName): Option[Set[ActorRef]] = pit.get(name)

  def remove(name: CCNName): Option[Set[ActorRef]] =  {println(s"Stacktrace: " + Thread.currentThread().getStackTrace().toString); pit.remove(name); }

  override def toString() = {
    pit.toString()
  }

//  def case Timeout(name, face) => {
//      pit.get(name) map { pendingFaces =>
//        logger.warning(s"Timing out interest: $name to face $face")
//        pit += name -> (pendingFaces - face)
//      }
//    }
}
