package nfn.service

import akka.actor.ActorRef
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}

class PandocService extends NFNService {
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
