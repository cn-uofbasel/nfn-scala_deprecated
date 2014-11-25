package production

import ccn.packet.CCNName
import com.typesafe.scalalogging.slf4j.Logging
import config.{StaticConfig, ComputeNodeConfig, RouterConfig}
import nfn.service.{WordCountService, PandocService}
import nfn.service.WordCountService
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

          val wc = new WordCountService()
          val pandoc = new PandocService
          node.publishService(wc)
          node.publishService(pandoc)
        }
        case _ => printUsageAndExit
      }
  }
}
