package nfn.service.NBody

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNStringValue, _}
import nfn.tools.Networking._


class RenderService extends NFNService {
  override def function(interestName: CCNName, argSeq: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {


    var options = Map('xres -> 500, 'yres -> 500)
    var configuration = Array[Byte]()

    var args = argSeq.toList
    while (args.nonEmpty) args match {
      case NFNStringValue("-w") :: NFNIntValue(value) :: tail => options += ('xres -> value); args = args drop 2
      case NFNStringValue("-h") :: NFNIntValue(value) :: tail => options += ('yres -> value); args = args drop 2
      case NFNContentObjectValue(name, data) :: tail => configuration = data; args = args drop 1
      case _ => args = List()
    }

    val dataString = new String(configuration)

//    println(s"xres: ${options('xres)}, yres: ${options('yres)}")
//    println("CONFIGURATION START")
//    println(configuration.toString)
//    println("MAYBE THIS ONE?")
//    println(dataString)
//    println("CONFIGURATION END")

    val deltaTime = 60
    val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
    val renderArea = Rect(-systemSize / 2, systemSize)
//    val config = Config.random(renderArea, 500)
    val config = Config.fromString(dataString)

    val simulation = new Simulation(config, deltaTime)

//    val resolution = Vector(options('xres), options('yres))
    val canvas = new BufferedImage(options('xres), options('yres), BufferedImage.TYPE_INT_RGB)
    simulation.render(renderArea, canvas)

    val baos = new ByteArrayOutputStream()
    ImageIO.write(canvas, "png", baos)
    val byteArray = baos.toByteArray

    NFNDataValue(byteArray)

//    var lastTime = System.currentTimeMillis()
//    val intermediateInterval = 500
//    var intermediateIndex = 0

//    simulation.run(stepCount, step => {
//      val currentTime = System.currentTimeMillis()
//      val elapsed = currentTime - lastTime
//      if (elapsed > intermediateInterval) {
//        intermediateResult(ccnApi, interestName, intermediateIndex, NFNStringValue(simulation.config.toString))
//        intermediateIndex += 1
//        lastTime = currentTime
//      }
//    })
//    NFNStringValue(simulation.config.toString)


  }
}

