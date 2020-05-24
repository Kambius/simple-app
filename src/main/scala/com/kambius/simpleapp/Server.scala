package com.kambius.simpleapp

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.kambius.simpleapp.repos.UsersRepo
import com.kambius.simpleapp.routes.UserRoutes
import com.kambius.simpleapp.services.{ReqRes, Users}
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext

object Server {
  final case class Config(serverPort: Int, dbConnectionUrl: String, dbUser: String, dbPassword: String)

  def stream[F[_]: ConcurrentEffect: ContextShift: Timer](config: Config, ec: ExecutionContext): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](ec).stream

      usersRepo = UsersRepo.impl[F](
        Transactor.fromDriverManager(
          "org.postgresql.Driver",
          config.dbConnectionUrl,
          config.dbUser,
          config.dbPassword
        )
      )

      reqRes             = ReqRes.impl[F](client)
      users              = Users.impl[F](usersRepo, reqRes)
      httpApp            = UserRoutes.routes(users).orNotFound
      httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F](ec)
        .bindHttp(config.serverPort, "0.0.0.0")
        .withHttpApp(httpAppWithLogging)
        .serve
    } yield exitCode
  }.drain
}
