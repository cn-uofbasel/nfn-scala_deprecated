package nfn.service.NBody

import java.awt.geom.Ellipse2D
import java.awt.{Color, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{File, PrintWriter}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Random

object Simulation {
  val G = 6.674E-11f
  def main(args: Array[String]): Unit = {
    println(s"The simulation is not intended to be run directly. " +
      s"This is only for testing purposes. " +
      s"Use the SimulationService on a compute server instead.")
    val stepCount: Int = 10000
    val bodyCount: Int = 1000
    val deltaTime: Float = 60
    val systemSize = Vector(Body.earth.radius * 1000f, Body.earth.radius * 1000f)
    val renderArea = Rect(-systemSize / 2, systemSize)
    val resolution = Vector(1000, 1000)

    val config = Config.random(renderArea, bodyCount)
    //config.writeToFile("~/Desktop/config.csv")
    //val config = Config.fromFile("~/Desktop/config.csv")
    val simulation = new Simulation(config, deltaTime)

    //simulation.render(renderArea, resolution, new java.io.File(s"~/Desktop/sim/sim00000.png"))
    simulation.run(stepCount, step => {
      println(s"Iteration $step")
      //simulation.config.writeToFile(f"~/Desktop/sim/csv$step%05d.csv")
      //simulation.render(renderArea, resolution, new java.io.File(f"~/Desktop/sim/sim$step%05d.png"))
    })
  }
}

class Simulation(var config: Config, val timeStep: Float) {
  def run(stepCount: Int, callback: (Int) => Unit): Unit = {
    for (step <- 1 to stepCount) {
      iterate()
      callback(step)
    }
  }
  def iterate() = {
    // Super simple iteration, that could obviously be optimized in different ways (e.g. Barnes-Hut sim),
    // but that's not the point here and would be counterproductive to testing long running computations.
    val newBodies = new ArrayBuffer[Body](config.bodies.length)
    for (i <- config.bodies.indices) {
      val b_i = config.bodies(i)
      var sum = Vector.zero
      for (j <- config.bodies.indices) {
        if (i != j) {
          val b_j = config.bodies(j)
          val distance = Vector.distance(b_j.position, b_i.position)
          val F = ((b_j.position - b_i.position) * Simulation.G * b_i.mass * b_j.mass) / Math.pow(distance, 3).toFloat
          sum += F
        }
      }
      val acceleration = sum / b_i.mass
      var velocity = b_i.velocity + acceleration * timeStep
      var position = b_i.position + velocity * timeStep

      // Check for collisions
      for (j <- config.bodies.indices) {
        if (i != j) {
          val b_j = config.bodies(j)
          val distance = Vector.distance(b_j.position, position)
          val collision = distance < (b_i.radius + b_j.radius) * 0.5
          if (collision) {
            velocity = (b_i.velocity * b_i.mass + b_j.velocity * b_j.mass) / (b_i.mass + b_j.mass)
            position = (b_i.position + b_j.position) / 2
            b_i.mass = if (i < j) b_i.mass + b_j.mass else 0
            b_j.mass = if (i > j) b_i.mass + b_j.mass else 0
          }
        }
      }
      if (b_i.mass > 0) {
        newBodies += new Body(position, velocity, b_i.mass)
      }
    }
    config = new Config(newBodies.toArray)
  }
  def render(area: Rect, resolution: Vector, file: File): Unit = {
    val canvas = new BufferedImage(resolution.x.toInt, resolution.y.toInt, BufferedImage.TYPE_INT_RGB)
    render(area, canvas)
    javax.imageio.ImageIO.write(canvas, "png", file)
  }
  def render(area:Rect, canvas: BufferedImage): Unit =  {
    val resolution = Vector(canvas.getWidth, canvas.getHeight())
    val proportion = resolution / area.size
    val origin = resolution / 2
    def bodyPosition(body: Body) = proportion * body.position + origin
    def bodySize(body: Body) = proportion * body.radius()
    val g = canvas.createGraphics()
    g.setColor(Color.WHITE)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    for (body <- config.bodies) {
      val size = bodySize(body)
      val position = bodyPosition(body) - size
      val shape = new Ellipse2D.Double(position.x, position.y, size.x * 2, size.y * 2)
      g.fill(shape)
    }
    g.dispose()
  }
}

object Config {
  def fromString(string: String) = new Config(
    string.split("\n") map {
      line => {
        val values = line.split(",") map (_.trim.toDouble)
        new Body(Vector(values(0), values(1)), Vector(values(2), values(3)), values(4))
      }
    }
  )
  def fromFile(filePath: String) = fromString(Source.fromFile(filePath).mkString)
  def random(area: Rect, count: Int) = new Config(Body.random(area, count))
}

class Config(val bodies: Array[Body]) {
  override def toString = bodies map {
    body => Array(body.position.x, body.position.y, body.velocity.x, body.velocity.y, body.mass) mkString ","
  } mkString "\n"
  def writeToFile(filePath: String) = {
    val writer = new PrintWriter(new File(filePath))
    writer.write(toString)
    writer.close()
  }
}

object Body {
  val density = 5515 // Earth's avg density in kg/m^3
  val earth = new Body(Vector(0, 0), 5.97E24f)
  val massSD = Body.earth.mass * 0.8f
  val randomizeInitialVelocity = false
  def random(area: Rect): Body = { // Return a body within the specified area with a mass between 0.1 and 10 earths
  val position: Vector = area.position + Vector.random() * area.size
    val mass: Double = Body.earth.mass + Math.min(10, Math.max(0.1, Random.nextGaussian())) * massSD
    val velocity = if (randomizeInitialVelocity) Vector(-position.y, -position.x) / position.length * 100000 else Vector.zero
    new Body(position, velocity, mass)
  }
  def random(area: Rect, count: Int): Array[Body] = Array.fill(count) { Body.random(area) }
}

class Body(var position: Vector, var velocity: Vector, var mass: Double) {
  val density: Double = Body.density
  def volume(): Double  = mass / density
  def radius(): Double = Math.pow((3 * volume()) / (4 * Math.PI), 1f / 3)
  def this(position: Vector, mass: Double) = this(position, Vector.zero, mass)
  override def toString = s"(position: (${position.x}, ${position.y}), velocity: (${velocity.x}, ${velocity.y}), mass: ${mass})"
}

object Vector {
  val zero = Vector(0, 0)
  def distance(a: Vector, b: Vector) = Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2))
  def random() = Vector(Random.nextFloat, Random.nextFloat())
}

case class Vector(x: Double, y: Double) {
  def unary_- = Vector(-this.x , -this.y)
  def +(that: Vector) = Vector(this.x + that.x, this.y + that.y)
  def -(that: Vector) = Vector(this.x - that.x, this.y - that.y)
  def *(that: Vector) = Vector(this.x * that.x, this.y * that.y)
  def /(that: Vector) = Vector(this.x / that.x, this.y / that.y)
  def *(that: Double) = Vector(this.x * that, this.y * that)
  def /(that: Double) = Vector(this.x / that, this.y / that)
  def length = Math.sqrt(x * x + y * y)
}

case class Rect(position: Vector, size: Vector)
