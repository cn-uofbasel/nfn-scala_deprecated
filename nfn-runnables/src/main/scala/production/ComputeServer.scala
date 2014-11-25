package production

import ccn.packet.CCNName
import config.{ComputeNodeConfig, RouterConfig}
import nfn.service.{WordCountService, PandocService}
import nfn.service.WordCountService
import node.LocalNode
import node.LocalNodeFactory.defaultMgmtSockNameForPrefix

object ComputeServer {

  def printUsageAndExit = {

    println("Usage: <prefix> <mgmtsocket> <ccn-lite-port> <compute-server-port> (e.g. /ndn/ch/unibas/ccn-lite 9000 9001)")

    sys.exit(1)
  }
  def main(args: Array[String]) = {
      args match {
        case Array(prefixStr, mgmtSocket, ccnlPortStr, computeServerPortStr) => {
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
