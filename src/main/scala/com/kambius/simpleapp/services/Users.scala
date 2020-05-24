package com.kambius.simpleapp.services

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.kambius.simpleapp.repos.UsersRepo
import com.kambius.simpleapp.services.ReqRes.{ReqResError, ReqResResponse, ReqResUser}
import com.kambius.simpleapp.services.Users._
import io.circe.Decoder
import io.circe.derivation._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.syntax.all._
import org.http4s.{EntityDecoder, Status}

trait Users[F[_]] {
  def add(userId: Int, email: String): F[Either[UserError, User]]
  def get(email: String): F[Either[NotFount, User]]
  def remove(email: String): F[Either[NotFount, Unit]]
}

object Users {
  def impl[F[_]: Sync](usersRepo: UsersRepo[F], reqRes: ReqRes[F]): Users[F] = new UsersImpl(usersRepo, reqRes)

  final case class User(email: String, id: Int, firstName: String, lastName: String)

  sealed trait UserError { def message: String }
  final case class NotFount(message: String)      extends UserError
  final case class AlreadyExists(message: String) extends UserError

  object NotFount {
    def byEmail(email: String): NotFount = NotFount(s"There is no user with email: $email")
  }
}

trait ReqRes[F[_]] {
  def getUser(userId: Int): F[Either[UserError, ReqResUser]]
}

object ReqRes {
  def impl[F[_]: Sync](httpClient: Client[F]): ReqRes[F] = new ReqResImpl(httpClient)

  final case class ReqResError(message: String) extends RuntimeException

  final case class ReqResResponse(data: ReqResUser)
  final case class ReqResUser(firstName: String, lastName: String)

  object ReqResResponse {
    implicit val reqResResponseDecoder: Decoder[ReqResResponse] =
      deriveDecoder(renaming.snakeCase)
    implicit def reqResResponseEntityDecoder[F[_]: Sync]: EntityDecoder[F, ReqResResponse] =
      jsonOf
  }

  object ReqResUser {
    implicit val reqResUserDecoder: Decoder[ReqResUser] = deriveDecoder(renaming.snakeCase)
  }
}

class ReqResImpl[F[_]](httpClient: Client[F])(implicit val F: Sync[F]) extends ReqRes[F] {
  override def getUser(userId: Int): F[Either[UserError, ReqResUser]] =
    httpClient
      .get(uri"https://reqres.in/api/users" / userId.toString) {
        case Status.Successful(r) =>
          r.as[ReqResResponse].map(_.data.asRight)

        case Status.NotFound(_) =>
          F.pure(NotFount(s"User with id $userId is not found in ReqRes").asLeft)

        case r =>
          F.raiseError(ReqResError(s"Failed to retrieve user from ReqRes. Response code: ${r.status.code}"))
      }
}

class UsersImpl[F[_]](usersRepo: UsersRepo[F], reqRes: ReqRes[F])(implicit val F: Sync[F]) extends Users[F] {
  def add(userId: Int, email: String): F[Either[UserError, User]] =
    reqRes
      .getUser(userId)
      .flatMap {
        case Right(userData) =>
          val user = User(email, userId, userData.firstName, userData.lastName)
          usersRepo.add(user).map { added =>
            if (added) user.asRight else AlreadyExists(s"User with email $email is already added").asLeft
          }

        case Left(e: UserError) =>
          F.pure(e.asLeft[User])
      }

  override def get(email: String): F[Either[NotFount, User]] =
    usersRepo.get(email).map {
      case Some(user) => user.asRight
      case None       => NotFount.byEmail(email).asLeft
    }

  override def remove(email: String): F[Either[NotFount, Unit]] =
    usersRepo.remove(email).map { deleted =>
      if (deleted)
        ().asRight
      else
        NotFount.byEmail(email).asLeft
    }
}
