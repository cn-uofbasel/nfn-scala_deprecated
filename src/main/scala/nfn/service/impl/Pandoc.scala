package nfn.service.impl

import akka.actor.ActorRef
import ccn.ccnlite.CCNLiteInterfaceCli
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}
import nfn.service._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class Pandoc extends NFNService {
  override def function: (Seq[NFNValue], ActorRef) => NFNValue = {
    (args, _) => args match {
      case Seq(NFNContentObjectValue(_, doc), NFNStringValue(from), NFNStringValue(to)) =>
        val cmds = List("pandoc", "-f", from, "-t", to)
        val futResult =
          SystemCommandExecutor(cmds, Some(doc)).execute map {
            case ExecutionSuccess(_, translatedDoc) => {
              NFNDataValue(translatedDoc.take(CCNLiteInterfaceCli.maxChunkSize))
            }
            case err: ExecutionError => throw new ServiceException(s"Error when executing pandoc system command: $err")
        }
       Await.result(futResult, 1.second)
      case _ => throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
