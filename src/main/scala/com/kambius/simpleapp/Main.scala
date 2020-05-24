package com.kambius.simpleapp

import cats.effect.{ExitCode, IO, IOApp}
import com.kambius.simpleapp.Server.Config

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val config = IO {
      val envPrefix = "SAMPLE_APP_"
      Config(
        serverPort = sys.env(envPrefix + "SERVER_PORT").toInt,
        dbConnectionUrl = sys.env(envPrefix + "DB_CONNECTION"),
        dbUser = sys.env(envPrefix + "DB_USER"),
        dbPassword = sys.env(envPrefix + "DB_PASSWORD")
      )
    }

    config.flatMap(Server.stream[IO](_, global).compile.drain.as(ExitCode.Success))
  }
}
