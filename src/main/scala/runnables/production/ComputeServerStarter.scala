package runnables.production


import ccn.packet.{CCNName, Content}
import com.typesafe.scalalogging.slf4j.Logging
import config.{ComputeNodeConfig, RouterConfig, StaticConfig}
import nfn.service.GPS.GPX.GPXOriginFilter
import nfn.service.GPS.GPX.GPXDistanceAggregator
import nfn.service.GPS.GPX.GPXDistanceComputer
import nfn.service.NBody
import nfn.service.Temperature.{ReadSensorData, ReadSensorDataSimu, StoreSensorData}
import nfn.service._
import node.LocalNode
import orgOpenmhealth.helperServices.SimpleToJSON
import orgOpenmhealth.services.{DistanceTo, PointCount}
import scopt.OptionParser

import sys.process._
import scala.io.Source


object ComputeServerConfigDefaults {
  val mgmtSocket = "/tmp/ccn-lite-mgmt.sock"
  val ccnLiteAddr = "127.0.0.1"
  val ccnlPort = 9000
  val computeServerPort = 9001
  val isCCNLiteAlreadyRunning = false
  val logLevel = "warning"
  val prefix = CCNName("nfn", "node")
}
case class ComputeServerConfig(prefix: CCNName = ComputeServerConfigDefaults.prefix,
                               mgmtSocket: Option[String] = None,
                               ccnLiteAddr: String = ComputeServerConfigDefaults.ccnLiteAddr,
                               ccnlPort: Int = ComputeServerConfigDefaults.ccnlPort,
                               computeServerPort: Int = ComputeServerConfigDefaults.computeServerPort,
                               isCCNLiteAlreadyRunning: Boolean = ComputeServerConfigDefaults.isCCNLiteAlreadyRunning,
                               logLevel: String = ComputeServerConfigDefaults.logLevel,
                               suite: String = "")


object ComputeServerStarter extends Logging {

  val argsParser =  new OptionParser[ComputeServerConfig]("") {
    override def showUsageOnError = true

    head("nfn-scala: compute-server starter", "v0.2.0")
    opt[String]('m', "mgmtsocket") action { (ms, c) =>
      c.copy(mgmtSocket = Some(ms))
    } text s"unix socket name for ccnl mgmt ops or of running ccnl, if not specified ccnl UDP socket is used (example: ${ComputeServerConfigDefaults.mgmtSocket})"
    opt[String]('a', "ccnl-addr") action { case (a, c) =>
      c.copy(ccnLiteAddr = a)
    } text s"address ccnl should use or address of running ccnl (default: ${ComputeServerConfigDefaults.ccnLiteAddr})"
    opt[Int]('o', "ccnl-port") action { case (p, c) =>
      c.copy(ccnlPort = p)
    } text s"unused port ccnl should use or port of running ccnl (default: ${ComputeServerConfigDefaults.ccnlPort})"
    opt[String]('s', "suite") action { case (s, c) =>
      c.copy(suite = s)
    } text s"wireformat to be used (default: ndntlv)"
    opt[Int]('p', "cs-port") action { case (p, c) =>
      c.copy(computeServerPort = p)
    } text s"port used by compute server, (default: ${ComputeServerConfigDefaults.computeServerPort})"
    opt[Unit]('r', "ccnl-already-running") action { (_, c) =>
      c.copy(isCCNLiteAlreadyRunning = true)
    } text s"flag to indicate that ccnl is already running and should not be started internally by nfn-scala"
    opt[Unit]('v', "verbose") action { (_, c) =>
      c.copy(logLevel = "info")
    } text "loglevel 'info'"
    opt[Unit]('d', "debug") action { (_, c) =>
      c.copy(logLevel = "debug")
    } text "loglevel 'debug'"
    opt[Unit]('h', "help") action { (_, c) =>
      showUsage
      sys.exit
      c
    } text { "prints usage" }
    arg[String]("<node-prefix>") validate {
      p => if(CCNName.fromString(p).isDefined) success else failure(s"Argument <node-prefix> must be a valid CCNName (e.g. ${ComputeServerConfigDefaults.prefix})")
    } action {
      case (p, c) => c.copy(prefix = CCNName.fromString(p).get)
    } text s"prefix of this node, all content and services are published with this name (example: ${ComputeServerConfigDefaults.prefix})"
  }

  def main(args: Array[String]) = {
    argsParser.parse(args, ComputeServerConfig()) match {
      case Some(config) =>
        StaticConfig.setDebugLevel(config.logLevel)

        println("Suite is", config.suite)
        if(config.suite != ""){
          StaticConfig.setWireFormat(config.suite)
        }

        logger.debug(s"config: $config")

        // Configuration of the router, sro far always ccn-lite
        // It requires the socket to the management interface, isCCNOnly = false indicates that it is a NFN node
        // and isAlreadyRunning tells the system that it should not have to start ccn-lite
        val routerConfig = RouterConfig(config.ccnLiteAddr, config.ccnlPort, config.prefix, config.mgmtSocket.getOrElse("") ,isCCNOnly = false, isAlreadyRunning = config.isCCNLiteAlreadyRunning)


        // This configuration sets up the compute server
        // withLocalAm = false tells the system that it should not start an abstract machine alongside the compute server
        val computeNodeConfig = ComputeNodeConfig("127.0.0.1", config.computeServerPort, config.prefix, withLocalAM = false)

        // Abstraction of a node which runs both the router and the compute server on localhost (over UDP sockets)
        val node = LocalNode(routerConfig, Some(computeNodeConfig))

        // Publish services
        // This will internally get the Java bytecode for the compiled services, put them into jar files and
        // put the data of the jar into a content object.
        // The name of this service is infered from the package structure of the service as well as the prefix of the local node.
        // In this case the prefix is given with the commandline argument 'prefixStr' (e.g. /node/nodeA/nfn_service_WordCount)
        node.publishServiceLocalPrefix(new WordCount())
        node.publishServiceLocalPrefix(new DelayedWordCount())
        node.publishServiceLocalPrefix(new IntermediateTest())
//        node.publishServiceLocalPrefix(new FetchContentTest())
        node.publishServiceLocalPrefix(new NBody.SimulationService())
        node.publishServiceLocalPrefix(new NBody.RenderService())
        node.publishServiceLocalPrefix(new NBody.SimulationRenderService())
        node.publishServiceLocalPrefix(new ChainIntermediates())
        node.publishServiceLocalPrefix(new PubSubBroker())
        node.publishServiceLocalPrefix(new ControlRequestTest())

        //node.publishServiceLocalPrefix(new Pandoc())
        //node.publishServiceLocalPrefix(new PDFLatex())
        //node.publishServiceLocalPrefix(new Reverse())
        node.publishServiceLocalPrefix(new Echo())
        node.publishServiceLocalPrefix(new ChunkTest())
        node.publishServiceLocalPrefix(new Waypoint())
//        node.publishServiceLocalPrefix(new EchoP())
        //node.publishServiceLocalPrefix(new GPXOriginFilter())
        //node.publishServiceLocalPrefix(new GPXDistanceComputer())
        //node.publishServiceLocalPrefix(new GPXDistanceAggregator())
        //node.publishServiceLocalPrefix(new ReadSensorData())

//        node.publishServiceLocalPrefix(new PointCount())
//        node.publishServiceLocalPrefix(new DistanceTo())
//        node.publishServiceLocalPrefix(new SimpleToJSON())

        //node.publishServiceLocalPrefix(new StoreSensorData())
        //node.publishServiceLocalPrefix(new ReadSensorData())

        // Gets the content of the ccn-lite tutorial
        //node += PandocTestDocuments.tutorialMd(node.localPrefix)
        // Publishes a very small two-line markdown file
        //node += PandocTestDocuments.tinyMd(node.localPrefix)

        //Read GPS Trackpoints for NDN Fit Experiment, uncomment if needed
       //val files =  ("ls trackpoints/" !!)
       //val filelist = files.split('\n')
       /*filelist.foreach(f => {
        val data = Source.fromFile(s"trackpoints/$f").mkString
        val num = f.substring(f.indexOf("_")+1, f.indexOf("."))
        node += Content(CCNName(s"/ndn/ch/unibas/NDNfit/Joe/internal/running/training/2015/02/04/gpx/p$num".substring(1).split("/").toList, None), data.getBytes)
      }
      )*/

      /*
      val files =  ("ls /home/claudio/trackpoints/" !!)
      val filelist = files.split('\n')

      filelist.foreach(f => {
        val data = Source.fromFile(s"/home/claudio/trackpoints/$f").mkString
        val num = f.substring(f.indexOf("_")+1, f.indexOf("."))
        node += Content(CCNName(s"/ndn/ch/unibas/NDNfit/hidden/run1/gpx/data/p$num".substring(1).split("/").toList, None), data.getBytes)
      }
      )
      */





      /*
       * --------------
       *  GPX SCENARIO
       * --------------
       *
       * Uncomment code above to put raw data into the cache!
       *
       * Arguments for this runnable: -m /tmp/mgmt.sock -o 9000 -p 9001 -d /nfn/node0
       *
       *
       * Test Requests:
       *
       *  ccn-lite-peek -w 10 -u 127.0.0.1/9000 /ndn/ch/unibas/NDNfit/hidden/run1/gpx/data/p1 | ccn-lite-pktdump -f2
       *  ccn-lite-simplenfn -w 10 -u 127.0.0.1/9000 "call 3 /nfn/node0/nfn_service_GPX_GPXOriginFilter '/run1/gpx/data' 1" | ccn-lite-pktdump -f2
       *  ccn-lite-simplenfn -w 10 -u 127.0.0.1/9000 "call 5 /nfn/node0/nfn_service_GPX_GPXDistanceComputer '/run1/gpx/data' 1 '/run1/gpx/data' 2" | ccn-lite-pktdump -f2
       *  ccn-lite-simplenfn -w 10 -u 127.0.0.1/9000 "call 3 /nfn/node0/nfn_service_GPX_GPXDistanceAggregator '/run1/gpx/data' 5" | ccn-lite-pktdump -f2
       *
       *
       */

      case None => sys.exit(1)
    }
  }
}
