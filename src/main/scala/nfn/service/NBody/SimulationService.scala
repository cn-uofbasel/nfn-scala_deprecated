package nfn.service.NBody

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNStringValue, _}
import nfn.tools.Networking._


class SimulationService extends NFNService {

  override def function(interestName: CCNName, argSeq: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    // /path/to/NBodySimulation /path/to/config.csv '-d <deltaTime> -c <stepCount>'

    var options = Map('deltaTime -> 60, 'stepCount -> 1000)
    var configuration = Array[Byte]()

    var args = argSeq.toList
    while (args.nonEmpty) args match {
      case NFNStringValue("-d") :: NFNIntValue(value) :: tail => options += ('deltaTime -> value); args = args drop 2
      case NFNStringValue("-c") :: NFNIntValue(value) :: tail => options += ('stepCount -> value); args = args drop 2
      case NFNContentObjectValue(name, data) :: tail => configuration = data; args = args drop 1
      case _ => args = List()
    }

//    var configString = ""
//    var optionString = ""
//    args foreach {
//      case doc: NFNContentObjectValue => configString = new String(doc.data)
//      case NFNStringValue(s) => optionString = s
//      case _ =>
//        throw new NFNServiceArgumentException(
//          s"Invalid arguments for $ccnName: $args. " +
//          s"Usage: /path/to/service /path/to/config.csv <delta time> <step count>")
//    }


//    println(s"Delta time: ${options('deltaTime)}, Step count: ${options('stepCount)}")
//    println("Config:")
//    println(configuration.toString)

//    return NFNIntValue(1)

    //    if (args.length != 1 && !args.head.isInstanceOf[NFNContentObjectValue]) {
    //      throw new NFNServiceArgumentException(
    //        s"$ccnName can only be applied to values of type NFNContentObjectValue and not $args")
    //    }

    //    val doc = args.head.asInstanceOf[NFNContentObjectValue]
    //    val str = new String(doc.data)
    val deltaTime = options('deltaTime)
    val stepCount = options('stepCount)

    val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
    val renderArea = Rect(-systemSize / 2, systemSize)
    val config = if (configuration.length <= 0) Config.random(renderArea, 100)
      else Config.fromString(configuration.toString)

    val simulation = new Simulation(config, deltaTime)

    var lastTime = System.currentTimeMillis()
    val intermediateInterval = 1000
    var intermediateIndex = 0


//    intermediateResult(ccnApi, interestName, 0, NFNStringValue(simulation.config.toString))

    simulation.run(stepCount, step => {
      val currentTime = System.currentTimeMillis()
      val elapsed = currentTime - lastTime
      if (elapsed > intermediateInterval) {
        intermediateResult(ccnApi, interestName, intermediateIndex, NFNStringValue(simulation.config.toString))
        intermediateIndex += 1
        lastTime = currentTime
      }
    })
    NFNStringValue(simulation.config.toString)

    //    case doc: NFNContentObjectValue => splitString(new String(doc.data))
    //    args map {
    //      case doc: NFNContentObjectValue => 3
    //      case _ => throw new NFNServiceArgumentException(
    //        s"$ccnName can only be applied to values of type NFNContentObjectValue and not $args")
    //    }

    //    NFNStringValue("")
  }
}

