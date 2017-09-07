package nfn.service.NBody

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, Interest}
import nfn.service.{NFNStringValue, _}
import nfn.tools.Networking.{fetchContentAndKeepalive, intermediateResult}


class SimulationRenderService extends NFNService {
  override def function(interestName: CCNName, argSeq: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    var options = Map('xres -> 500, 'yres -> 500)
//    var configuration = Array[Byte]()
    var simulationName = "/node6/nfn_service_NBody_SimulationService/(@x call 1 x)/NFN"

    def parseArgs(): Unit ={
      var args = argSeq.toList
      while (args.nonEmpty) args match {
        case NFNStringValue("-w") :: NFNIntValue(value) :: tail => options += ('xres -> value); args = args drop 2
        case NFNStringValue("-h") :: NFNIntValue(value) :: tail => options += ('yres -> value); args = args drop 2
        case NFNStringValue(value) :: Nil => simulationName = value; args = args drop 1
        case _ => args = List()
      }
    }

    def renderConfig(content: Content): NFNValue = {
      val dataString = new String(content.data)

      val deltaTime = 60
      val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
      val renderArea = Rect(-systemSize / 2, systemSize)
      //val config = Config.random(renderArea, 500)
      val config = Config.fromString(dataString)

      val simulation = new Simulation(config, deltaTime)

      //val resolution = Vector(options('xres), options('yres))
      val canvas = new BufferedImage(options('xres), options('yres), BufferedImage.TYPE_INT_RGB)
      simulation.render(renderArea, canvas)

      val baos = new ByteArrayOutputStream()
      ImageIO.write(canvas, "png", baos)
      val byteArray = baos.toByteArray

      NFNDataValue(byteArray)
    }

    def runSimulation(): NFNValue = {
      val simulationComponents = simulationName.stripPrefix("/").split("/").toList map {
        component => component.replace("%2F", "/")
      }
      val simulationInterest = Interest(CCNName(simulationComponents, None))

      val intermediateHandler = (index: Int, content: Content) => {
        println(s"Intermediate handler: $index")
        intermediateResult(ccnApi, interestName, index, renderConfig(content))
      }

      fetchContentAndKeepalive(ccnApi, simulationInterest, handleIntermediate = Some(intermediateHandler)) match {
        case Some(content) => renderConfig(content)
        case _ => NFNStringValue("Simulation could not be completed.")
      }
    }

    parseArgs()
    runSimulation()
  }
}

