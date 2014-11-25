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
          SystemCommandExecutor(List(cmds), Some(doc)).execute() match {
            case ExecutionSuccess(_, translatedDoc) =>  NFNDataValue(translatedDoc)
            case err: ExecutionError => throw new ServiceException(s"Error when executing pandoc system command: $err")
        }
      case _ => throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
