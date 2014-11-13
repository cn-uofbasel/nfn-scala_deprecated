package ccn.ccnlite

import java.io.File

import ccn.{CCNWireFormat, CCNInterface}
import ccn.packet._
import com.typesafe.scalalogging.slf4j.Logging
import myutil.IOHelper
import myutil.systemcomandexecutor._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Failure, Success}

case class CCNLiteInterfaceCli(wireFormat: CCNWireFormat) extends CCNInterface with Logging {
  val ccnLiteEnv = {
    val maybeCcnLiteEnv = System.getenv("CCNL_HOME")
    if(maybeCcnLiteEnv == null) {
      throw new Exception("CCNL_HOME system variable is not set. Set it to the root directory of ccn-lite.")
    }
    maybeCcnLiteEnv
  }
  val utilFolderName = s"$ccnLiteEnv/util/"


  private def ccnNameToRoutableNameAndNfnString(name: CCNName): List[String] = {
    val nameCmps = name.cmps
    if(nameCmps.last == "NFN") {
      List(
        nameCmps.take(nameCmps.size - 2).mkString("/", "/", ""),
        nameCmps(nameCmps.size - 2)
      )
    } else {
      List(nameCmps.mkString("/", "/", ""))
    }
  }

  override def mkBinaryInterest(interest: Interest)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    val mkI = "ccn-lite-mkI"
    val cmds: List[String] = List(utilFolderName+mkI, "-s", wireFormat.toString) ++ ccnNameToRoutableNameAndNfnString(interest.name)
    SystemCommandExecutor(cmds).execute map {
      case ExecutionSuccess(_, data) => data
      case execErr: ExecutionError =>
        throw new Exception(s"Error when creating binary interest for $interest: $execErr")
    }
  }

  override def mkBinaryContent(content: Content)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    val mkC = "ccn-lite-mkC"
    val cmds = List(utilFolderName+mkC, "-s", wireFormat.toString) ++ ccnNameToRoutableNameAndNfnString(content.name)
    SystemCommandExecutor(cmds, Some(content.data)).execute map {
      case ExecutionSuccess(_, data) => data
      case execErr: ExecutionError =>
        throw new Exception(s"Error when creating binary content for $content: $execErr")
    }
  }

  override def wireFormatDataToXmlPacket(binaryPacket: Array[Byte])(implicit ec: ExecutionContext): Future[CCNPacket] = {
    val pktdump = "ccn-lite-pktdump"
    val cmds = List(utilFolderName+pktdump, "-f", "1")

    SystemCommandExecutor(cmds, Some(binaryPacket)).execute map {
      case ExecutionSuccess(_, data) =>
        val xmlPacket = new String(data)
        CCNLiteXmlParser.parseCCNPacket(xmlPacket) match {
        case Success(packet) => packet
        case Failure(ex) => throw ex
      }
      case execErr: ExecutionError =>
        throw new Exception(s"Error when parsing xml for binary data: $execErr")
    }
  }

  override def mkAddToCacheInterest(content: Content)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    mkBinaryContent(content) flatMap { binaryContent =>
      val filename = s"./service-library/${content.name.hashCode}-${System.nanoTime}-${Random.nextInt()}.ccnb"
      val file = new File(filename)

      IOHelper.writeToFile(file, binaryContent)
      val ccnbAbsoluteFilename: String = file.getCanonicalPath

      val ctrl = "ccn-lite-ctrl"
      val cmds = List(utilFolderName + ctrl, "-m", "addContentToCache", ccnbAbsoluteFilename)
      val futCacheInterest = SystemCommandExecutor(cmds).execute map {
        case ExecutionSuccess(_, data) => data
        case execErr: ExecutionError =>
          throw new Exception(s"Error creating add to cache request: $execErr")
      }
      futCacheInterest.onComplete {
        case _ => file.delete()
      }
      futCacheInterest
    }
  }
}
