package nfn.service

import java.io.File

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content}
import myutil.IOHelper
import myutil.systemcomandexecutor._


object PandocTestDocuments {
  def tinyMd(prefix: CCNName) =
    Content(prefix.append("docs", "tiny_md"),
      """
        |# TODO List
        |* ~~NOTHING~~
      """.stripMargin.getBytes)



  def tutorialMd(prefix: CCNName) = {
    // Read the tutorial form the ccn-lite documentation and publish it
    val ccnlTutorialMdPath = "tutorial/tutorial.md"

    val tutorialMdName = prefix.append(CCNName("docs", "tutorial_md"))
    val ccnlHome = System.getenv("CCNL_HOME")
    val tutorialMdFile = new File(s"$ccnlHome/$ccnlTutorialMdPath")
    val tutorialMdData = IOHelper.readByteArrayFromFile(tutorialMdFile)
    Content(tutorialMdName, tutorialMdData)
  }
}

class Pandoc extends NFNService {
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(NFNContentObjectValue(_, doc), NFNStringValue(from), NFNStringValue(to)) =>
        val cmds = List("pandoc", "-s", "-f", from, "-t", to)
        SystemCommandExecutor(List(cmds), Some(doc)).execute() match {
          case ExecutionSuccess(_, translatedDoc) =>  NFNDataValue(translatedDoc)
          case ExecutionError(_, errMsgData, _) => NFNStringValue(s"Error when executing pandoc service: ${new String(errMsgData)}")
        }
      case _ =>
        throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
