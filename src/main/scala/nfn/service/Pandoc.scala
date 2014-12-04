package nfn.service

import akka.actor.ActorRef
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}

class Pandoc extends NFNService {
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(NFNContentObjectValue(_, doc), NFNStringValue(from), NFNStringValue(to)) =>
        val cmds = List("pandoc", "-f", from, "-t", to)
        SystemCommandExecutor(List(cmds), Some(doc)).execute() match {
          case ExecutionSuccess(_, translatedDoc) =>  NFNDataValue(translatedDoc)
          case ExecutionError(_, errMsgData, _) => NFNStringValue(s"Error when executing pandoc service: ${new String(errMsgData)}")
        }
      case _ =>
        throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
