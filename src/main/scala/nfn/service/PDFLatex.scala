package nfn.service

import java.io.{FilenameFilter, File}

import akka.actor.ActorRef
import myutil.IOHelper
import myutil.systemcomandexecutor._

import scala.concurrent.ExecutionContext

class PDFLatex extends NFNService {
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(NFNContentObjectValue(_, doc)) =>

        val dir = new File(s"./temp-service-library/${IOHelper.uniqueFileName("pdflatex")}")
        if(!dir.exists()) dir.mkdirs()
        val cmds = List("pdflatex", s"-output-directory=${dir.getCanonicalPath}")
        implicit val ec = ExecutionContext.Implicits.global
        SystemCommandExecutor(List(cmds), Some(doc)).executeWithTimeout() match {
          case ExecutionSuccess(_, translatedDoc) =>
            dir.list().find(_.endsWith(".pdf")) match {
              case Some(pdfFile) => NFNDataValue(IOHelper.readByteArrayFromFile(new File(dir + "/" + pdfFile)))
              case None => NFNStringValue(s"Error when executing pdflatex, " +
                "the resulting pdf could not be created, but pdflatex executed without error")
            }
          case ExecutionError(_, errData, _) => NFNStringValue(s"Error when executing pdflatex: ${new String(errData)}")
        }
      case _ => throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
