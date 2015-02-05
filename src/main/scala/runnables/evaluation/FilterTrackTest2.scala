package runnables.evaluation

import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}
import monitor.Monitor
import nfn.service.FilterTrack
import node.LocalNodeFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object FilterTrackTest2 extends App {

  implicit val conf: Config = ConfigFactory.load()

  /*
  *
  * SETUP:
  *  Network with dsu, dpu and dvu.
  *  Service "FilterTrack" on dpu
  *  Sample data "track" on dsu
  *

             [track]        {filterTrack}
               |                  |
           +-------+          +-------+
   <perm>--|  dsu  |**********|  dpu  |
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
  *  "FilterTrack" on configurable
  *  access level.
  *
  * */

  // network setup
  val dsu = LocalNodeFactory.forId(1)
  val dpu = LocalNodeFactory.forId(2)
  val dvu = LocalNodeFactory.forId(3)
  val nodes = List(dsu, dpu, dvu)
  dsu <~> dpu
  dpu <~> dvu
  dvu <~> dsu

  // service setup
  val filterTrackServ = new FilterTrack()
  dpu.publishServiceLocalPrefix(filterTrackServ)
  val filterTrack = dpu.localPrefix.append(filterTrackServ.ccnName)

  // data setup
  val trackname = dsu.localPrefix.append("track")
  val trackdata = "3 4 6 4 4 6 4 5 6 4 6 7 6 6 5 5 6 5".getBytes
  dsu += Content(trackname, trackdata)

  // setup permissions
  // TODO

  // --------------------------------------------------------------

  Thread.sleep(1000)
  import lambdacalculus.parser.ast.LambdaDSL._
  import nfn.LambdaNFNImplicits._
  implicit val useThunks: Boolean = false

  val interest_raw: Interest = filterTrack call(trackname, 0)
  val interest_northpole: Interest = filterTrack call(trackname, 1)

  // send interest_raw or interest_northpole?
  val interest = interest_raw

  // send interest...
  val startTime = System.currentTimeMillis
  println(s" | Send interest: " + interest)
  dvu ? interest onComplete {
    // ... and receive content
    case Success(resultContent) => {
      println(s" | Result:        " + new String(resultContent.data))
      println(s" | Time:          " + (System.currentTimeMillis-startTime) + "ms")
      Monitor.monitor ! Monitor.Visualize()
      nodes foreach { _.shutdown() }
    }
    // ... but do not get content
    case Failure(e) =>
      println(" | No content received.")
      Monitor.monitor ! Monitor.Visualize()
      nodes foreach { _.shutdown() }
  }

}
