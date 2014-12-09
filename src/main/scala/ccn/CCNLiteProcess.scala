package ccn

import java.io._
import com.typesafe.scalalogging.slf4j.Logging
import config.{CCNLiteSystemPath, RouterConfig, StaticConfig}
import ccn.packet.CCNName
import myutil.systemcomandexecutor.{ExecutionError, ExecutionSuccess, SystemCommandExecutor}

/**
 * Pipes a [[InputStream]] to a file with the given name into ./log/<name>.log.
 * If you don't want the logfile to be overridden, set appendSystemTime to true.
 * @param is input stream to pipe the content to a file
 * @param logname the name of the file (./log/logname)
 * @param appendTimestamp Avoids overriding of the file by writing to ./log/<logname>-timestamp.log instead
 */
class LogStreamReaderToFile(is: InputStream, logname: String, appendTimestamp: Boolean = false) extends Runnable {

  private val reader = new BufferedReader(new InputStreamReader(is))
  val filename = {
    val logFolderFile = new File("./log")
    if(logFolderFile.exists && logFolderFile.isFile) {
      logFolderFile.delete
    }
    if(!new File("./log").exists) {
      logFolderFile.mkdir
    }
    if (appendTimestamp) s"./log/$logname.log"
    else s"./log/$logname-${System.currentTimeMillis}.log"
  }

  private val writer = new BufferedWriter(new FileWriter(new File(filename)))

  override def run() = {
    try {
      var line = reader.readLine
      while (line != null) {
        writer.write(line + "\n")
        line = reader.readLine
      }
    } finally {
      reader.close()
      writer.close()
    }
  }
}


/**
 * Encapsulates a native ccn-lite-relay process.
 * Starts a process with the given port and sets up a compute port. All output is written to a file in './log/ccnlite-<host>-<port>.log.
 * Call start and stop.
 * @param nodeConfig
 */
case class CCNLiteProcess(nodeConfig: RouterConfig) extends Logging {

  val wireFormat = StaticConfig.packetformat

  case class NetworkFace(toHost: String, toPort: Int) {

    val mgmtCmds = nodeConfig.mgmntSocket match {
      case "" => List("-u", s"${nodeConfig.host}", s"${nodeConfig.port}")
      case mgmntSock @ _ => List("-x", mgmntSock)
    }

    val newFaceCommand = List("newUDPface", "any", s"$toHost", s"$toPort")

    val cmdUDPFace = s"$ccnLiteEnv/bin/ccn-lite-ctrl" :: mgmtCmds ++ newFaceCommand
    val cmdMgmtToXml = List(s"$ccnLiteEnv/bin/ccn-lite-ccnb2xml")

    val faceId: Int =
      SystemCommandExecutor(List(cmdUDPFace, cmdMgmtToXml)).execute() match {
        case ExecutionSuccess(_, faceIdData) => {
          val faceIdStr = new String(faceIdData)
          logger.debug(s"faceIdStr: $faceIdStr")
          if(faceIdStr.contains("failed")) {
            throw new Exception(s"Error when registering new udp face")
          } else {
            val start = faceIdStr.indexOf("<FACEID>") + "<FACEID>".length
            val end = faceIdStr.indexOf("</FACEID>")
            val faceId = Integer.parseInt(faceIdStr.substring(start, end))
            logger.info(s"Registered udp face with faceid $faceId")
            faceId
          }
        }
        case err: ExecutionError => throw new Exception(s"Error when registering new udp face: $err")
      }

    udpFaces += (toHost -> toPort) -> this


    def registerPrefix(prefixToRegister: String) = {
      val prefixRegCommand = List("prefixreg", s"$prefixToRegister", s"$faceId", s"$wireFormat")
      val cmdPrefixReg =  s"$ccnLiteEnv/bin/ccn-lite-ctrl" :: mgmtCmds ++ prefixRegCommand
      SystemCommandExecutor(List(cmdPrefixReg)).execute() match {
        case ExecutionSuccess(_, xml) => logger.info(s"Registered prefix for $prefixToRegister")
        case err: ExecutionError => logger.error(s"Error when registering prefix: $err")
      }
    }

    def unregisterPrefixPrefix(prefixToRegister: String) = {
      udpFaces.get((host, port)) map { updFace =>
        val cmdPrefixReg =  s"$ccnLiteEnv/bin/ccn-lite-ctrl -x $sockName prefixunreg $prefixToRegister $faceId"
        logger.debug(s"CCNLiteProcess-$prefix: executing '$cmdPrefixReg")
        Runtime.getRuntime.exec(cmdPrefixReg.split(" "))
      }
    }
  }

  val ccnLiteEnv = CCNLiteSystemPath.ccnLiteHome

  private var process: Process = null

  val host = nodeConfig.host
  val port = nodeConfig.port
//  val computePort = nodeConfig.computePort
  val prefix = nodeConfig.prefix.toString

  val sockName = nodeConfig.mgmntSocket

  var udpFaces:Map[(String, Int), NetworkFace] = Map()
  val processName = if(nodeConfig.isCCNOnly) "CCNLiteNFNProcess" else "CCNLiteProcess"

  def start() = {

    if(!nodeConfig.isAlreadyRunning) {
      val ccnliteExecutableName = if(nodeConfig.isCCNOnly) s"$ccnLiteEnv/ccn-lite-relay" else s"$ccnLiteEnv/ccn-nfn-relay"
      val ccnliteExecutable = ccnliteExecutableName + (if(StaticConfig.isNackEnabled) "-nack" else "")


      val mgmtCmdsOrEmpty =
        if(sockName != "") {
          List("-x", s"$sockName")
        } else Nil

      val cmds: List[String] = s"$ccnliteExecutable" :: List("-v", "98", "-u", "$port", "-s", s"$wireFormat") ++ mgmtCmdsOrEmpty
      logger.debug(s"$processName-$prefix: executing: '${cmds.mkString(" ")}'")
      val processBuilder = new ProcessBuilder(cmds:_*)
      processBuilder.redirectErrorStream(true)
      process = processBuilder.start

      val lsr = new LogStreamReaderToFile(process.getInputStream, s"ccnlite-$host-$port", appendTimestamp = true)
      val thread = new Thread(lsr, s"LogStreamReader-$prefix")
      thread.start()
    }
  }

  def stop() = {
    if (process != null) {
      process.destroy()
    }
  }

  private def getOrCreateNetworkFace(host: String, port: Int): NetworkFace = {
    udpFaces.getOrElse(host -> port, NetworkFace(host, port))
  }

  def connect(otherNodeConfig: RouterConfig): Unit = {
    getOrCreateNetworkFace(otherNodeConfig.host, otherNodeConfig.port).registerPrefix(otherNodeConfig.prefix.toString)
  }


  def addPrefix(prefix: CCNName, gatewayHost: String, gatewayPort: Int) = {
    getOrCreateNetworkFace(gatewayHost, gatewayPort).registerPrefix(prefix.toString)
  }

  def removePrefix(prefix: CCNName, gatewayHost: String, gatewayPort: Int) = {
    getOrCreateNetworkFace(host, port).unregisterPrefixPrefix(prefix.toString)
  }

}
