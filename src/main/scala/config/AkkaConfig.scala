package config

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object AkkaConfig {

  implicit def timeout(timeouteMillis:Int) = Timeout(timeouteMillis, TimeUnit.MILLISECONDS)


  def config(debuglevel: String) =
    ConfigFactory.parseString(s"""
      |akka.loglevel=$debuglevel
      |akka.debug.lifecycle=on
      |akka.debug.receive=on
      |akka.debug.event-stream=on
      |akka.debug.unhandled=on
      |akka.log-dead-letters=10
      |akka.log-dead-letters-during-shutdown=on
    """.stripMargin
    )
}
