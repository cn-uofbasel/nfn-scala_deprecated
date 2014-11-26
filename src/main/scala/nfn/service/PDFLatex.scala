package nfn.service

import java.io.{FilenameFilter, File}

import akka.actor.ActorRef
import myutil.IOHelper
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}

class PDFLatex extends NFNService {
  override def function: (Seq[NFNValue], ActorRef) => NFNValue = {
    (args, _) => args match {
      case Seq(NFNContentObjectValue(_, doc)) =>

        val dir = new File(s"./service-library/${IOHelper.uniqueFileName("pdflatex")}")

        if(!dir.exists()) dir.mkdirs()
        val cmds = List("pdflatex", s"-output-directory=${dir.getCanonicalPath}")
        SystemCommandExecutor(List(cmds), Some(doc)).execute() match {
          case ExecutionSuccess(_, translatedDoc) =>
            dir.list().find(_.endsWith(".pdf")) match {
              case Some(pdfFile) => NFNDataValue(IOHelper.readByteArrayFromFile(pdfFile))
              case None => NFNStringValue(s"Error when executing pdflatex, " +
                "the resulting pdf could not be created, but pdflatex executed without error")
            }
          case ExecutionError(_, errData, _) => NFNStringValue(s"Error when executing pdflatex: ${new String(errData)}")
        }
      case _ => throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
