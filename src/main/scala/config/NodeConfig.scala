package config

import java.io.File
import java.util.concurrent.TimeUnit

import akka.event.Logging.LogLevel
import akka.util.Timeout
import ccn.CCNWireFormat
import ccn.packet.CCNName
import com.typesafe.config.ConfigException.BadValue
import com.typesafe.config.{Config, ConfigFactory}
import monitor.Monitor.NodeLog

import scala.concurrent.duration.Duration

case class ConfigException(msg: String) extends Exception(msg)

object StaticConfig {

  private var maybeConfig: Option[Config] = None

  private var maybeDebugLevel: Option[String] = Option.empty[String]

  private var wireFormat: Option[CCNWireFormat] = None

  def config: Config = maybeConfig match {
    case Some(config) => config
    case None =>
      val conf = ConfigFactory.load()
      maybeConfig = Some(conf)
      conf
  }

  def isNackEnabled =  config.getBoolean("nfn-scala.usenacks")

  def isThunkEnabled = config.getBoolean("nfn-scala.usethunks")

  def defaultTimeoutDuration = Duration(config.getInt("nfn-scala.defaulttimeoutmillis"), TimeUnit.MILLISECONDS)

  def defaultTimeout = Timeout(defaultTimeoutDuration)

  def debugLevel = {
    val lvl =  maybeDebugLevel.getOrElse(config.getString("nfn-scala.debuglevel"))
    LogLevelSLF4J.setLogLevel(lvl)
    lvl
  }

  def setDebugLevel(lvl: String) = {
    maybeDebugLevel = Some(lvl)
    LogLevelSLF4J.setLogLevel(lvl)
  }

  def setWireFormat(wf: String) = {
    wireFormat = CCNWireFormat.fromName(wf)
  }

  def packetformat: CCNWireFormat = {
    wireFormat match{
      case Some(wf) => wf
      case None => {
        val path = "nfn-scala.packetformat"
        val wfName = config.getString(path)
        CCNWireFormat.fromName(wfName) match {
          case Some(wf) => wf
          case None => throw new BadValue(path,
            s"""
               | WireFormat cannot be "$wfName"
        """.stripMargin)
        }
      }
    }
  }
}

object LogLevelSLF4J {
  import org.slf4j.LoggerFactory
  import ch.qos.logback.classic.{Level, Logger}
  def setLogLevel(str: String) = {
    val slf4jLevel =
      str.toUpperCase match {
        case "DEBUG" =>
          Level.DEBUG
        case "INFO" =>
          Level.INFO
        case "WARNING" =>
          Level.WARN
        case "ERROR" =>
          Level.ERROR
        case _ =>
          Level.INFO
      }

    setSLF4J(slf4jLevel)
  }
  def setSLF4J(level: Level) {
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    rootLogger.setLevel(level)
  }
}

object CCNLiteSystemPath {
  val ccnLiteHome: String = {
      val maybeCcnLiteEnv = System.getenv("CCNL_HOME")
      if(maybeCcnLiteEnv == null || maybeCcnLiteEnv == "") {
        if(new File("./ccn-lite-nfn/bin").exists()) {
          new File("./ccn-lite-nfn").getCanonicalPath
        } else {
          throw new Exception("nfn-scala ccn-lite submodule was not initialzed (either git clone --recursive or git submodule init && git submodule update) or CCNL_HOME was not set")
        }
      } else maybeCcnLiteEnv
  }
}

trait NodeConfig {
  def host: String
  def port: Int
  def prefix: CCNName
  def toNodeLog: NodeLog


}

case class CombinedNodeConfig(maybeNFNNodeConfig: Option[RouterConfig], maybeComputeNodeConfig: Option[ComputeNodeConfig])

case class RouterConfig(host: String, port: Int, prefix: CCNName, mgmntSocket: String = "", isCCNOnly: Boolean = false, isAlreadyRunning: Boolean = false, defaultNFNRoute: String = "") extends NodeConfig {
  def toNodeLog: NodeLog = NodeLog(host, port, Some(if(isCCNOnly) "CCNNode" else "NFNNode"), Some(prefix.toString))
}


case class ComputeNodeConfig(host: String, port: Int, prefix: CCNName, withLocalAM: Boolean = false) extends NodeConfig {
  def toNodeLog: NodeLog = NodeLog(host, port, Some("ComputeNode"), Some(prefix + "compute"))
}
