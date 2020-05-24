package com.kambius.simpleapp.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.either._
import cats.syntax.option._
import com.kambius.simpleapp.services.Users
import com.kambius.simpleapp.services.Users.{AlreadyExists, NotFount, User, UserError}
import io.circe.Json
import io.circe.literal._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.syntax.kleisli._
import org.http4s.syntax.literals._
import org.http4s.{Method, Request, Status}
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class UserRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  private val users = new Users[IO] {
    override def add(userId: Int, email: String): IO[Either[UserError, User]] = IO {
      email match {
        case "some@email.com"     => User("some@email.com", 2, "Bob", "Bobson").asRight
        case "existing@email.com" => AlreadyExists("User already added").asLeft
        case _                    => NotFount.byEmail(email).asLeft
      }
    }

    override def get(email: String): IO[Either[NotFount, User]] = IO {
      email match {
        case "some@email.com"  => User("some@email.com", 2, "Bob", "Bobson").asRight
        case "error@email.com" => throw new RuntimeException("some error")
        case _                 => NotFount.byEmail(email).asLeft
      }
    }

    override def remove(email: String): IO[Either[NotFount, Unit]] = IO {
      email match {
        case "some@email.com" => ().asRight
        case _                => NotFount.byEmail(email).asLeft
      }
    }
  }

  private val routes = UserRoutes.routes[IO](users)

  private def check(req: Request[IO])(expectedStatus: Status, expectedBody: Option[Json]): IO[Assertion] =
    routes.orNotFound.run(req).flatMap { resp =>
      resp.status shouldBe expectedStatus
      expectedBody match {
        case Some(expected) =>
          resp.as[Json].map(_ shouldBe expected)
        case None =>
          resp.body.compile.toVector.map(_ shouldBe empty)
      }
    }

  "UserRoutes" - {
    "get user" in {
      check(Request(method = Method.GET, uri = uri"/users/some@email.com"))(
        Status.Ok,
        json"""{
          "email": "some@email.com",
          "id": 2,
          "first_name": "Bob",
          "last_name": "Bobson"
        }""".some
      )
    }

    "add user" in {
      check(
        Request(
          method = Method.POST,
          uri = uri"/users"
        ).withEntity(json"""{ "user_id": 2, "email": "some@email.com" }"""))(
        Status.Created,
        json"""{ "email": "some@email.com" }""".some
      )
    }

    "remove user" in {
      check(Request(method = Method.DELETE, uri = uri"/users/some@email.com"))(Status.Ok, None)
    }

    "validate user" in {
      check(
        Request(
          method = Method.POST,
          uri = uri"/users"
        ).withEntity(json"""{ "user_id": -2, "email": "@email.com" }"""))(
        Status.BadRequest,
        json"""{ "message": "Validation errors: email: @email.com is not a valid email, user_id: -2 is negative." }""".some
      )
    }

    "handle internal errors" in {
      check(Request(method = Method.GET, uri = uri"/users/error@email.com"))(
        Status.InternalServerError,
        json"""{ "message": "some error" }""".some
      )
    }

    "handle not found errors" in {
      check(Request(method = Method.GET, uri = uri"/users/error@email.com/unknown"))(
        Status.NotFound,
        json"""{ "message": "Not found" }""".some
      )
    }
  }
}
