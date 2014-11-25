package production

import ccn.packet.CCNName
import config.{ComputeNodeConfig, RouterConfig}
import nfn.service.impl.{Pandoc, WordCountService}
import node.{ComputeNode, LocalNode}

object ComputeServer {

  def printUsageAndExit = {

    println("Usage: <prefix> <ccn-lite-port> <compute-server-port> (e.g. /ndn/ch/unibas/ccn-lite 9000 9001)")

    sys.exit(1)
  }
  def main(args: Array[String]) = {
      args match {
        case Array(prefixStr, ccnlPortStr, computeServerPortStr) => {
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
          val routerConfig = RouterConfig("127.0.0.1", ccnlPort, prefix, isCCNOnly = false, isAlreadyRunning = true)

          val computeNodeConfig = ComputeNodeConfig("127.0.0.1", computeServerPort, prefix, withLocalAM = false)

          val node = LocalNode(routerConfig, Some(computeNodeConfig))

          val wc = new WordCountService()
          val pandoc = new Pandoc
          node.publishService(wc)
          node.publishService(pandoc)
        }
      }
  }
}
