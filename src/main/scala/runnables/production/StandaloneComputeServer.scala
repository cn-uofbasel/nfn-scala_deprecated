package runnables.production


import ccn.packet.CCNName
import com.typesafe.scalalogging.slf4j.Logging
import config.{ComputeNodeConfig, RouterConfig, StaticConfig}
import nfn.service._
import node.LocalNode




object StandaloneComputeServer extends Logging {



  def printUsageAndExit = {

    println("Usage: <prefix> <mgmtsocket> <ccn-lite-addr>:<ccn-lite-port> <compute-server-port> <loglevel> (e.g. /ndn/ch/unibas/ccn-lite /tmp/ccn-lite-1.sock 9000 9001 info)")

    sys.exit(1)
  }
  def main(args: Array[String]) = {
    println(s"Standalone ComputeServer started with ${args.toList.map(_.toString)}")
    args match {
      case Array(prefixStr, mgmtSocket, ccnlUrlStr, computeServerPortStr, loglevel) => {
        val (prefix: CCNName, ccnlHost:String, ccnlPort: Int, computeServerPort: Int) =
        try {
          val Array(ccnlHost, ccnlPortStr) = ccnlUrlStr.split(":")
          (
            CCNName(prefixStr.split("/").tail:_*),
            ccnlHost,
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

        // Configuration of the router, so far always ccn-lite
        // It requires the socket to the management interface, isCCNOnly = false indicates that it is a NFN node
        // and isAlreadyRunning = true tells the system that it should not have to start ccn-lite
        val routerConfig = RouterConfig(ccnlHost, ccnlPort, prefix, mgmtSocket ,isCCNOnly = false, isAlreadyRunning = true)


        // This configuration sets up the compute server
        // withLocalAm = false tells the system that it should not start an abstract machine alongside the compute server
        // because we know that the ccn-lite node will be started in nfn mode
        val computeNodeConfig = ComputeNodeConfig("127.0.0.1", computeServerPort, prefix, withLocalAM = false)

        // Abstraction of a node which runs both the router and the compute server on localhost (over UDP sockets)
        val node = LocalNode(routerConfig, Some(computeNodeConfig))


        // Publish services
        // This will internally get the Java bytecode for the compiled services, put them into jar files and
        // put the data of the jar into a content object.
        // The name of this service is infered from the package structure of the service as well as the prefix of the local node.
        // In this case the prefix is given with the commandline argument 'prefixStr' (e.g. /node/nodeA/nfn_service_WordCount)
        node.publishServiceLocalPrefix(new WordCount())
        node.publishServiceLocalPrefix(new Pandoc())
        node.publishServiceLocalPrefix(new Reverse())


        node += PandocTestDocuments.tutorialMd(node.localPrefix)
        node += PandocTestDocuments.tinyMd(node.localPrefix)
      }
      case _ => printUsageAndExit
    }
  }
}
