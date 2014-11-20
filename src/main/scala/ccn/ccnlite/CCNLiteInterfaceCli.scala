package ccn.ccnlite

import java.io.File

import ccn.{CCNWireFormat, CCNInterface}
import ccn.packet._
import com.typesafe.scalalogging.slf4j.Logging
import myutil.IOHelper
import myutil.systemcomandexecutor._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Failure, Success}

object CCNLiteInterfaceCli {
  val maxChunkSize = 4096
}

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

    if(nameCmps.size > 0 && nameCmps.last == "NFN") {
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

    val chunkCmps = interest.name.chunkNum match {
      case Some(chunkNum) => List("-n", s"$chunkNum")
      case None => Nil
    }
    val cmds: List[String] = List(utilFolderName+mkI, "-s", s"$wireFormat") ++ chunkCmps ++ ccnNameToRoutableNameAndNfnString(interest.name)
    SystemCommandExecutor(cmds).execute map {
      case ExecutionSuccess(_, data) => data
      case execErr: ExecutionError =>
        throw new Exception(s"Error when creating binary interest for $interest: $execErr")
    }
  }

  override def mkBinaryContent(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]] = {
    mkBinaryContent(content, CCNLiteInterfaceCli.maxChunkSize)
  }
  def mkBinaryContent(content: Content, chunkSize: Int)(implicit ec: ExecutionContext): Future[List[Array[Byte]]] = {
    val mkC = "ccn-lite-mkC"
    val baseCmds = List(utilFolderName+mkC, "-s", s"$wireFormat")

    // split into chunk size
    val dataChunks = content.data.grouped(chunkSize).toList

    val lastChunkNum = dataChunks.size - 1

    if(dataChunks.size == 1) {
      val cmds = baseCmds ++ ccnNameToRoutableNameAndNfnString(content.name)
      SystemCommandExecutor(cmds, Some(content.data)).execute map {
        case ExecutionSuccess(_, data) => List(data)
        case execErr: ExecutionError =>
          throw new Exception(s"Error when creating binary content for $content: $execErr")
      }
    } else {
//      List[Future...] -> Future[List...]
      Future.sequence {
        dataChunks.zipWithIndex.map { case (chunkedData, chunkNum) =>
          val cmds = baseCmds ++ List("-n", s"$chunkNum", "-l" , s"$lastChunkNum") ++ ccnNameToRoutableNameAndNfnString(content.name)
          // create binary chunked content object for each
          SystemCommandExecutor(cmds, Some(chunkedData)).execute map {
            case ExecutionSuccess(_, data: Array[Byte]) => data
            case execErr: ExecutionError =>
              throw new Exception(s"Error when creating binary content for chunk $content: $execErr")
          }
        }
      }
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

  override def mkAddToCacheInterest(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]] = {
    val chunkSize = 150
    logger.debug(s"add to cache for $content")
    mkBinaryContent(content, chunkSize) flatMap { (binaryContents: List[Array[Byte]]) =>
      Future.sequence {
        binaryContents map { binaryContent =>
          val filename = s"./service-library/${content.name.hashCode}-${System.nanoTime}-${Random.nextInt()}.ccnb"
          val file = new File(filename)

          IOHelper.writeToFile(file, binaryContent)
          val ccnbAbsoluteFilename: String = file.getCanonicalPath

          val ctrl = "ccn-lite-ctrl"
          val cmds = List(utilFolderName + ctrl, "-m", "addContentToCache", ccnbAbsoluteFilename)
          val futCacheInterest: Future[Array[Byte]] = SystemCommandExecutor(cmds).execute map {
            case ExecutionSuccess(_, data) => data
            case execErr: ExecutionError =>
              throw new Exception(s"Error creating add to cache request: $execErr")
          }
          futCacheInterest.onComplete { _ =>  file.delete() }
          futCacheInterest
        }
      }
    }
  }
}
