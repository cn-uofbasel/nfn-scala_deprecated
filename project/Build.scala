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
    resolvers += Resolver.sonatypeRepo("public"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    test in assembly := {},
    MainBuild.compileCCNLite
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
        "org.apache.bcel" % "bcel" % "5.2",
        "com.github.scopt" %% "scopt" % "3.3.0"
      ),
      mainClass in assembly := Some("runnables.production.ComputeServerStarter")
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
    )
  )

  lazy val testservice: Project = Project(
    "testservice",
    file("testservice"),
    settings = buildSettings
  ).dependsOn(nfn)

  val compileCCNLiteTask = TaskKey[Unit]("compileCCNLite")
  val compileCCNLite = compileCCNLiteTask := {
    val ccnlPath = {
      val p = System.getenv("CCNL_HOME")
      if(p == null || p == "") {
        if(new File("./ccn-lite-nfn/bin").exists()) {
          new File("./ccn-lite-nfn").getCanonicalPath
        } else {
          throw new Exception("CCNL_HOME was not set and nfn-scala ccn-lite submodule was not initialzed (either git clone --recursive or git submodule init && git submodule update)")
        }
      } else p
    }

    val processBuilder = {
      val cmds = List("make", "clean", "all")

      new java.lang.ProcessBuilder(cmds:_*)
    }
    val ccnlPathFile = new File(s"$ccnlPath/src")
    println(s"Building in directory $ccnlPathFile")
    processBuilder.directory(ccnlPathFile)
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
      line = reader.readLine
    }
  }
}

