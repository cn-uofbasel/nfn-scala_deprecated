package nfn.service.NBody

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNStringValue, _}
import nfn.tools.Networking._


class SimulationService extends NFNService {

  override def function(interestName: CCNName, argSeq: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    // /path/to/NBodySimulation [/path/to/config] ['-c' <configSize>] ['-d' <deltaTime>] ['-s' <stepCount>] ['-i' <intermediateInterval>]

    var options = Map('configSize -> 10, 'deltaTime -> 60, 'stepCount -> 1000, 'intermediateInterval -> 0)
    var configuration = Array[Byte]()

    var args = argSeq.toList
    while (args.nonEmpty) args match {
      case NFNStringValue("-c") :: NFNIntValue(value) :: tail => options += ('configSize -> value); args = args drop 2
      case NFNStringValue("-d") :: NFNIntValue(value) :: tail => options += ('deltaTime -> value); args = args drop 2
      case NFNStringValue("-s") :: NFNIntValue(value) :: tail => options += ('stepCount -> value); args = args drop 2
      case NFNStringValue("-i") :: NFNIntValue(value) :: tail => options += ('intermediateInterval -> value); args = args drop 2
      case NFNContentObjectValue(name, data) :: tail => configuration = data; args = args drop 1
      case _ => args = List()
    }

    val configSize = options('configSize)
    val deltaTime = options('deltaTime)
    val stepCount = options('stepCount)
    val intermediateInterval = options('intermediateInterval) * 1000

    println("intermediateInterval: " + intermediateInterval)

    val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
    val renderArea = Rect(-systemSize / 2, systemSize)
    val config = if (configuration.length <= 0) Config.random(renderArea, configSize)
      else Config.fromString(configuration.toString)

    val simulation = new Simulation(config, deltaTime)

    var lastTime = System.currentTimeMillis()
    var intermediateIndex = 0


//    intermediateResult(ccnApi, interestName, 0, NFNStringValue(simulation.config.toString))

    simulation.run(stepCount, step => {
      val currentTime = System.currentTimeMillis()
      val elapsed = currentTime - lastTime
      if (elapsed > intermediateInterval && intermediateInterval > 0) {
        intermediateResult(ccnApi, interestName, intermediateIndex, NFNStringValue(simulation.config.toString))
        intermediateIndex += 1
        lastTime = currentTime
      }
    })
    NFNStringValue(simulation.config.toString)

  }
}

