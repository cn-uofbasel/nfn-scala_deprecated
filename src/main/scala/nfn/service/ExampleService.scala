package nfn.service
import akka.actor.ActorRef
import ccn.packet._
import nfn.tools.Networking._
import scala.concurrent.duration._
import scala.io.Source

class ExampleService extends NFNService
{
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef) : NFNValue =
  {
    args match{
      case Seq (name: NFNStringValue) => {
        return NFNStringValue(name.str)
      }
      case _ => return NFNStringValue("error")
    }
  }
}
