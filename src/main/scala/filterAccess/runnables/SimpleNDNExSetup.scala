package filterAccess.runnables

import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}
import filterAccess.json._
import filterAccess.service.{KeyChannel, AccessChannel, ContentChannel}
import monitor.Monitor
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object SimpleNDNExSetup extends App {

  implicit val conf: Config = ConfigFactory.load()

  /*
  *
  * SETUP:
  *  Network with dsu, dpu and dvu.
  *  Service "ContentChannel" on dpu
  *  Sample data "track" and permissions on dsu
  *

        {track.KeyChannel}
      {track.AccessChannel}     {track.ContentChannel}
                 |                  |
             +-------+          +-------+
    [track]--|  dsu  |**********|  dpu  |
             +-------+          +-------+
                   *              *
                    *            *
                     *          *
                      +-------+
                      |  dvu  |
                      +-------+


  *
  * SCENARIO:
  *  dvu requests "track" filtered with
  *  "ContentChannel" on configurable
  *  access level.
  *
  * */


  // network setup
  val dsu = LocalNodeFactory.forId(1)
  val dpu = LocalNodeFactory.forId(2)
  val dvu = LocalNodeFactory.forId(3, isCCNOnly = true)
  val nodes = List(dsu, dpu, dvu)
  dsu <~> dpu
  dpu <~> dvu
  dvu <~> dsu

  // service setup (content channel)
  val filterTrackServ = new ContentChannel
  dpu.publishServiceLocalPrefix(filterTrackServ)
  val contentTrack = dpu.localPrefix.append(filterTrackServ.ccnName)

  // service setup (access/permission channel)
  val accessTrackServ = new AccessChannel
  dpu.publishServiceLocalPrefix(accessTrackServ)
  val accessTrack = dpu.localPrefix.append(accessTrackServ.ccnName)

  // service setup (key channel)
  val keyTrackServ = new KeyChannel
  dpu.publishServiceLocalPrefix(keyTrackServ)
  val keyTrack = dpu.localPrefix.append(keyTrackServ.ccnName)

  // setup track data
  val trackName = dsu.localPrefix.append("track")
  val trackData = ContentChannelBuilder.buildTrack(
    List(
      TrackPoint(3, 4, 6),
      TrackPoint(4, 4, 6),
      TrackPoint(4, 5, 6),
      TrackPoint(4, 6, 7),
      TrackPoint(6, 6, 5),
      TrackPoint(5, 6, 5)
    ),
    "/the/trackname"
  ).getBytes

  dsu += Content(trackName, trackData)

  // setup permission data
  val permissionName = dsu.localPrefix.append("trackPermission")
  val permissionData = AccessChannelBuilder.buildPermissions(
    List(
      UserLevel("user1", 0),
      UserLevel("user2", 1),
      UserLevel("processor", 0)),
    "/node/node1/permissionTrack"
  ).getBytes

  dsu += Content(permissionName, permissionData)

  // setup key data
  val keyName = dsu.localPrefix.append("trackKey")
  val keyData = KeyChannelBuilder.buildKeys(
    List(
      (AccessLevel(0), LevelKey(99)),
      (AccessLevel(1), LevelKey(44))
    ),
    "/key"
  ).getBytes

  println("==========")
  println(new String(keyData))
  println("==========")



  dsu += Content(keyName, keyData)

  // --------------------------------------------------------------

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

  // --------------------------------------------------------------

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

  // --------------------------------------------------------------

  println("=== FETCH KEY FROM DPU ===")

  Thread.sleep(1000)

  val interest_key:Interest = keyTrack call("/node/node1/trackPermission", "user2", 1)

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
