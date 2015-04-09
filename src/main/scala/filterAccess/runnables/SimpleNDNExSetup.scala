package filterAccess.runnables

import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}
import filterAccess.json._
import filterAccess.dataGenerator.SimpleNDNExData
import filterAccess.service.access.LegacyAccessChannel
import filterAccess.service.content.LegacyContentChannel
import filterAccess.service.key.LegacyKeyChannel
import monitor.Monitor
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object SimpleNDNExSetup extends App {

  implicit val conf: Config = ConfigFactory.load()

  /*
   * Created by Claudio Marxer <marxer@claudio.li>
   *
   * SETUP:
   *  Network with dsu, dpu and dvu.
   *  Service "LegacyContentChannel" on dpu
   *  Sample data "track" and "permissions" on dsu
   *

           {track.LegacyKeyChannel}
           {track.LegacyAccessChannel}     {track.LegacyContentChannel}
                      |                  |
                  +-------+          +-------+
         [track]--|  dsu  |**********|  dpu  |
    [permission]  +-------+          +-------+
                        *              *
                         *            *
                          *          *
                           +-------+
                           |  dvu  | <--- Sends out Interests...
                           +-------+           (CCN Only)


   *
   * SCENARIO:
   *  dvu requests "track" filtered with
   *  "LegacyContentChannel" on configurable
   *  access level.
   *
   * */



  // -----------------------------------------------------------------------------
  // ==== NETWORK SETUP ==========================================================
  // -----------------------------------------------------------------------------

  // network setup
  val dsu = LocalNodeFactory.forId(1)
  val dpu = LocalNodeFactory.forId(2)
  val dvu = LocalNodeFactory.forId(3, isCCNOnly = true)
  val nodes = List(dsu, dpu, dvu)
  dsu <~> dpu
  dpu <~> dvu
  dvu <~> dsu



  // -----------------------------------------------------------------------------
  // ==== SERVICE SETUP ==========================================================
  // -----------------------------------------------------------------------------

  // service setup (content channel)
  val filterTrackServ = new LegacyContentChannel
  dpu.publishServiceLocalPrefix(filterTrackServ)
  val contentTrack = dpu.localPrefix.append(filterTrackServ.ccnName)

  // service setup (access/permission channel)
  val accessTrackServ = new LegacyAccessChannel
  dpu.publishServiceLocalPrefix(accessTrackServ)
  val accessTrack = dpu.localPrefix.append(accessTrackServ.ccnName)

  // service setup (key channel)
  val keyTrackServ = new LegacyKeyChannel
  dpu.publishServiceLocalPrefix(keyTrackServ)
  val keyTrack = dpu.localPrefix.append(keyTrackServ.ccnName)



  // -----------------------------------------------------------------------------
  // ==== DATA SETUP =============================================================
  // -----------------------------------------------------------------------------

  // setup track data
  val trackName = dsu.localPrefix.append("track")
  val trackData = SimpleNDNExData.generateTrack("/data/trackname")
  dsu += Content(trackName, trackData)

  // setup permission data
  val permissionName = dsu.localPrefix.append("track").append("permission")
  val permissionData = SimpleNDNExData.generatePermissions("/node/node1/track/permission")
  dsu += Content(permissionName, permissionData)

  // setup key data
  val keyName = dsu.localPrefix.append("track").append("key")
  val keyData = SimpleNDNExData.generateKeys("/node/node1/track/key", 3)
  dsu += Content(keyName, keyData)



  // -----------------------------------------------------------------------------
  // ==== FETCH DATA FROM DVU ====================================================
  // -----------------------------------------------------------------------------

  println("=== FETCH DATA FROM DVU ===")

  Thread.sleep(1000)
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val interest_raw: Interest = contentTrack call(trackName, 1)
  val interest_northpole: Interest = contentTrack call(trackName, 1)

  // send interest_raw or interest_northpole?
  val interest = interest_raw

  // send interest for track from dvu...
  val startTime1 = System.currentTimeMillis
  println(s" |>> Send interest: " + interest)
  dpu ? interest onComplete {
    // ... and receive content
    case Success(resultContent) => {
      println(s" | Result:        " + new String(resultContent.data))
      println(s" | Time:          " + (System.currentTimeMillis-startTime1) + "ms")
      Monitor.monitor ! Monitor.Visualize()
    }
    // ... but do not get content
    case Failure(e) =>{
      println(" | No content received.")
      Monitor.monitor ! Monitor.Visualize()
    }
  }



  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSIONS FROM DPU =============================================
  // -----------------------------------------------------------------------------

  println("=== FETCH PERMISSIONS FROM DPU ===")

  Thread.sleep(1000)

  val interest_permissions:Interest = accessTrack call("user1 track", 0)

  // send interest for permissions from dpu...
  val startTime2 = System.currentTimeMillis
  println(s" | Send interest: " + interest_permissions)
  dpu ? interest_permissions onComplete {
    // ... and receive content
    case Success(resultContent) => {
      println(s" | Result:        " + new String(resultContent.data))
      println(s" | Time:          " + (System.currentTimeMillis-startTime2) + "ms")
      Monitor.monitor ! Monitor.Visualize()
    }
    // ... but do not get content
    case Failure(e) => {
      println(" | No content received.")
      Monitor.monitor ! Monitor.Visualize()
    }
  }



  // -----------------------------------------------------------------------------
  // ==== FETCH KEY FROM DPU =====================================================
  // -----------------------------------------------------------------------------

  println("FETCH KEY FROM DPU")

  Thread.sleep(1000)

  val interest_key:Interest = keyTrack call("/node/node1/track", "user1", 0)

  // send interest for permissions from dpu...
  val startTime3 = System.currentTimeMillis
  println(s" | Send interest: " + interest_key)
  dpu ? interest_key onComplete {
    // ... and receive content
    case Success(resultContent) => {
      println(s" | Result:        " + new String(resultContent.data))
      println(s" | Time:          " + (System.currentTimeMillis-startTime3) + "ms")
      Monitor.monitor ! Monitor.Visualize()
      Thread.sleep(20000)
      nodes foreach { _.shutdown() }
    }
    // ... but do not get content
    case Failure(e) => {
      println(" | No content received.")
      Monitor.monitor ! Monitor.Visualize()
      nodes foreach {
        _.shutdown()
      }
    }
  }

}
