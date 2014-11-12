package ccnliteinterface.cli

import java.io._

import ccnliteinterface._
import com.typesafe.scalalogging.slf4j.Logging
import org.omg.IOP.IORHelper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by basil on 03/09/14.
 */
case class CCNLiteInterfaceCli(wireFormat: CCNLiteWireFormat) extends CCNLiteInterface with Logging {

  val ccnLiteEnv = {
    val maybeCcnLiteEnv = System.getenv("CCNL_HOME")
    if(maybeCcnLiteEnv == null) {
      throw new Exception("CCNL_HOME system variable is not set. Set it to the root directory of ccn-lite.")
    }
    maybeCcnLiteEnv
  }

  val utilFolderName = s"$ccnLiteEnv/util/"

  private def executeCommandToByteArray(cmds: Array[String], maybeDataToPipe: Option[Array[Byte]]): (Array[Byte], Array[Byte]) = {

    def futInToOut(is: InputStream, os: OutputStream): Future[Unit] = {
      Future(
        Iterator.continually(is.read)
                .takeWhile(_ != -1)
                .foreach(os.write)
      )
    }
    val rt = Runtime.getRuntime

    logger.debug(s"Executing: ${cmds.mkString("'_'")}")
    val proc = rt.exec(cmds)

    val procIn = new BufferedInputStream(proc.getInputStream)
    val resultOut = new ByteArrayOutputStream()
    val futResult = futInToOut(procIn, resultOut)

    val procErrIn = new BufferedInputStream(proc.getErrorStream)
    val errorOut = new ByteArrayOutputStream()
    val futError = futInToOut(procErrIn, errorOut)

    maybeDataToPipe map { dataToPipe =>
      val procOut = new BufferedOutputStream(proc.getOutputStream)
      procOut.write(dataToPipe)
      procOut.close()
    }

    proc.waitFor()

    while(! (futResult.isCompleted && futError.isCompleted)) {
      Thread.sleep(20)
    }

    val result = resultOut.toByteArray
    val err = errorOut.toByteArray

//    logger.debug(s"Res (ret=${proc.exitValue()},size=${result.size}): ${new String(result)}")
    if(err.size > 0) {
      val errString = s"${cmds.mkString(" ")}: ${new String(err)} (size=${err.size})"
      if(proc.exitValue() == 0) { logger.debug(errString) }
      else { logger.error(errString) }
    }
    proc.destroy()

    (result, err)
  }
//  def wireFormatNum(f: CCNLiteWireFormat) = wireFormat match {
//    case CCNBWireFormat() => 0
//    case NDNTLVWireFormat() => 2
//  }

  def nameCmpsToRoutableNameAndNfnString(nameCmps: Array[String]): Array[String] = {

    logger.debug(s"nameCmpsToNFNName ${nameCmps.toList}")
      val a = if(nameCmps.last == "NFN") {
        Array(
          nameCmps.take(nameCmps.size - 2).mkString("/", "/", ""),
          nameCmps(nameCmps.size - 2)
        )
      } else {
        Array(nameCmps.mkString("/", "/", ""))
      }

    logger.debug(s"created: ${a.toList}")
    a
  }

  override def mkBinaryInterest(nameCmps: Array[String]): Array[Byte] = {
    val mkI = "ccn-lite-mkI"
    val cmds: Array[String] = Array(utilFolderName+mkI, "-s", wireFormat.toString) ++ nameCmpsToRoutableNameAndNfnString(nameCmps)

    val (res, _) = executeCommandToByteArray(cmds, None)
    res
  }

  override def mkBinaryContent(name: Array[String], data: Array[Byte]): Array[Byte] = {
    val mkC = "ccn-lite-mkC"
    val cmds = Array(utilFolderName+mkC, "-s", wireFormat.toString) ++ nameCmpsToRoutableNameAndNfnString(name)
    val (res, _) = executeCommandToByteArray(cmds, Some(data))
    res
  }

  override def ccnbToXml(binaryPacket: Array[Byte]): String = {
    val pktdump = "ccn-lite-pktdump"
    val cmds = Array(utilFolderName+pktdump, "-f", "1")

    val (res, _) = executeCommandToByteArray(cmds, Some(binaryPacket))

    new String(res)
  }


  override def mkAddToCacheInterest(ccnbAbsoluteFilename: String): Array[Byte] = {

    val ctrl = "ccn-lite-ctrl"

    val cmds = Array(utilFolderName+ctrl, "-m", "addContentToCache", ccnbAbsoluteFilename)

    val (res, err) = executeCommandToByteArray(cmds, None)

    res
  }
}
