import java.io._

import sbt.File
import sbt._
import Keys._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.assemblySettings

object BuildSettings {
  val buildSettings: Seq[Def.Setting[_]] = assemblySettings ++ Seq (
    version       := "0.1-SNAPSHOT",
    scalaVersion  := "2.10.3",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "UTF-8", "-language:implicitConversions"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    test in assembly := {}
  )
}

object MainBuild extends Build {

  import BuildSettings._

  lazy val nfn: Project = Project(
    "nfn",
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.2.1",
        "com.typesafe.akka" %% "akka-testkit" % "2.2.1",
        "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.3",
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "com.typesafe" % "config" % "1.2.1",
        "org.slf4j" % "slf4j-api" % "1.7.5",
        "net.liftweb" %% "lift-json" % "2.5.1",
        "org.apache.bcel" % "bcel" % "5.2"
      )
//      mainClass in "nfn-scala-experiments" in Compile := Some("production.ComputeServer")
//      mainClass in (Compile, run) := Some("production.ComputeServer")
    )
  ).dependsOn(lambdaCalculus)

  lazy val lambdaCalculus: Project = Project(
    "lambdacalc",
    file("lambdacalc"),
    settings = buildSettings ++ Seq (
      libraryDependencies ++= Seq(
        "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.3",
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "org.slf4j" % "slf4j-api" % "1.7.5"
      ),
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
//      mainClass in (Compile, run) := Some("lambdacalculus.LambdaCalculus")
    )
  )

  lazy val lambdaMacros: Project = {
    val paradiseVersion = "2.0.0-M3"
    Project(
      "lambda-macros",
      file("lambda-macros"),
      settings = buildSettings ++ Seq(
        addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
        libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
        libraryDependencies ++= (
          if (scalaVersion.value.startsWith("2.10")) List("org.scalamacros" % "quasiquotes"  % paradiseVersion cross CrossVersion.full)
          else Nil
          )
      )
    ).dependsOn(lambdaCalculus)
  }

  lazy val nfnRunnables: Project = Project(
    "nfn-runnables",
    file("nfn-runnables"),
    settings = buildSettings
  ).dependsOn(nfn)

  lazy val testservice: Project = Project(
    "testservice",
    file("testservice"),
    settings = buildSettings
  ).dependsOn(nfn)

  val compileCCNLiteTask = TaskKey[Unit]("compileCCNLite")
  val compileCCNLite = compileCCNLiteTask := {
    val ccnlPath = {
      val p = System.getenv("CCNL_HOME")
      if(p == null) throw new Exception("CCNL_HOME no set. Get a copy of the current ccn-lite version from 'https://github.com/cn-uofbasel/ccn-lite' and set the variable to its path.")
      else p
    }

    val processBuilder = {
      val cmds = List("make", "clean", "all")

      new java.lang.ProcessBuilder(cmds:_*)
    }
    processBuilder.directory(new File(s"$ccnlPath"))
    val e = processBuilder.environment()
    e.put("USE_NFN", "1")
    e.put("USE_NACKS", "1")
    val process = processBuilder.start()
    val processOutputReaderPrinter = new InputStreamToStdOut(process.getInputStream)
    val t = new Thread(processOutputReaderPrinter).start()
    process.waitFor()
    val resVal = process.exitValue()
    if(resVal == 0)
      println(s"Compiled ccn-lite with return value ${process.exitValue()}")
    else
      throw new Exception("Error during compilation of ccn-lite")
    process.destroy()
  }
}

class InputStreamToStdOut(is: InputStream) extends Runnable {
  override def run(): Unit = {
    val reader = new BufferedReader(new InputStreamReader(is))
    var line = reader.readLine
    while(line != null) {
      println(reader.readLine())
      line = reader.readLine
    }
  }
}

