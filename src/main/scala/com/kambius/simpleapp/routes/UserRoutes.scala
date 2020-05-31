package com.kambius.simpleapp.routes

import cats.data.OptionT
import cats.effect.Sync
import cats.instances.option._
import cats.instances.string._
import cats.syntax.all._
import com.kambius.simpleapp.common.validation._
import com.kambius.simpleapp.services.Users
import com.kambius.simpleapp.services.Users.{AlreadyExists, NotFount, User}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.log4s._

object UserRoutes {
  private[this] val logger = getLogger

  def routes[F[_]](users: Users[F])(implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    val r = HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req
          .as[CreateUserDto]
          .flatMap { d =>
            d.validate match {
              case Some(errors) =>
                val errorMsg = errors
                  .map(f => s"${f.fieldName}: ${f.message}")
                  .mkString_("Validation errors: ", ", ", ".")
                BadRequest(ErrorDto(errorMsg))

              case None =>
                users.add(d.userId, d.email).flatMap {
                  case Right(user) =>
                    Created(Json.obj("email" -> Json.fromString(user.email)))

                  case Left(e: NotFount) =>
                    NotFound(ErrorDto(e.message))

                  case Left(e: AlreadyExists) =>
                    Conflict(ErrorDto(e.message))
                }
            }
          }

      case GET -> Root / email =>
        users.get(email).flatMap {
          case Right(user) =>
            Ok(UserDto.fromUser(user))

          case Left(NotFount(msg)) =>
            NotFound(ErrorDto(msg))
        }

      case DELETE -> Root / email =>
        users.remove(email).flatMap {
          case Right(()) =>
            Ok()

          case Left(NotFount(msg)) =>
            NotFound(ErrorDto(msg))
        }
    }

    val routesWithErrorHandler = HttpRoutes[F] { req =>
      OptionT.liftF {
        r.run(req)
          .value
          .flatMap {
            case None       => NotFound(ErrorDto("Not found"))
            case Some(resp) => F.pure(resp)
          }
          .handleErrorWith { t =>
            F.delay(logger.error(t)("Error during handling request")) *>
              InternalServerError(ErrorDto(t.getMessage))
          }
      }
    }

    Router("users" -> routesWithErrorHandler)
  }

  final case class ErrorDto(message: String)
  object ErrorDto {
    implicit val errorRespEncoder: Encoder[ErrorDto] =
      deriveEncoder(renaming.snakeCase)
    implicit def errorRespEntityEncoder[F[_]]: EntityEncoder[F, ErrorDto] =
      jsonEncoderOf
  }

  final case class CreateUserDto(email: String, userId: Int)
  object CreateUserDto {
    implicit val createUserDtoDecoder: Decoder[CreateUserDto] =
      deriveDecoder(renaming.snakeCase)
    implicit def createUserDtoEntityDecoder[F[_]: Sync]: EntityDecoder[F, CreateUserDto] =
      jsonOf

    implicit val validator: Validator[CreateUserDto] = dto =>
      Email.validate(dto.email, "email") |+| NonNegative[Int].validate(dto.userId, "user_id")
  }

  final case class UserDto(email: String, id: Int, firstName: String, lastName: String)
  object UserDto {
    def fromUser(u: User): UserDto = UserDto(u.email, u.id, u.firstName, u.lastName)

    implicit val userEncoder: Encoder[UserDto]                      = deriveEncoder(renaming.snakeCase)
    implicit def userEntityEncoder[F[_]]: EntityEncoder[F, UserDto] = jsonEncoderOf
  }
}
