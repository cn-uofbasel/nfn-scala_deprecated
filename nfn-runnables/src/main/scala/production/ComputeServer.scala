package production

import java.io.File

import ccn.packet.{Content, CCNName}
import com.typesafe.scalalogging.slf4j.Logging
import config.{StaticConfig, ComputeNodeConfig, RouterConfig}
import myutil.IOHelper
import nfn.service.{WordCount, Pandoc}
import nfn.service.WordCount
import node.LocalNode




object ComputeServer extends Logging {



  def printUsageAndExit = {

    println("Usage: <prefix> <mgmtsocket> <ccn-lite-port> <compute-server-port> <loglevel> (e.g. /ndn/ch/unibas/ccn-lite /tmp/ccn-lite-1.sock 9000 9001 info)")

    sys.exit(1)
  }
  def main(args: Array[String]) = {
      args match {
        case Array(prefixStr, mgmtSocket, ccnlPortStr, computeServerPortStr, loglevel) => {
          val (prefix: CCNName, ccnlPort: Int, computeServerPort: Int) =
          try {
            (
              CCNName(prefixStr.split("/").tail:_*),
              Integer.parseInt(ccnlPortStr),
              Integer.parseInt(computeServerPortStr)
            )
          } catch {
            case e: Exception =>
              sys.error(e.getMessage)
              printUsageAndExit
          }

          StaticConfig.setDebugLevel(loglevel)

          logger.info("Starting standalone ComputeServer")
          val routerConfig = RouterConfig("127.0.0.1", ccnlPort, prefix, mgmtSocket ,isCCNOnly = false, isAlreadyRunning = true)

          val computeNodeConfig = ComputeNodeConfig("127.0.0.1", computeServerPort, prefix, withLocalAM = false)

          val node = LocalNode(routerConfig, Some(computeNodeConfig))

          val wc = new WordCount()
          val pandoc = new Pandoc
          node.publishService(wc)
          node.publishService(pandoc)
          node += Content(node.prefix.append("docs", "tinymd"),
            """
              |# TODO List
              |* ~~NOTHING~~
            """.stripMargin.getBytes)

          val ccnlTutorialMdPath = "doc/tutorial/tutorial.md"

          val tutorialMdName = node.prefix.append(CCNName("docs", "tutorialmd"))
          val ccnlHome = System.getenv("CCNL_HOME")
          val tutorialMdFile = new File(s"$ccnlHome/$ccnlTutorialMdPath")
          val tutorialMdData = IOHelper.readByteArrayFromFile(tutorialMdFile)
          node += Content(tutorialMdName, tutorialMdData)
        }
        case _ => printUsageAndExit
      }
  }
}
