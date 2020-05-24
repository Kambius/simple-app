package com.kambius.simpleapp.services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.either._
import com.kambius.simpleapp.repos.InMemoryUsersRepo
import com.kambius.simpleapp.services.ReqRes.ReqResUser
import com.kambius.simpleapp.services.Users.{AlreadyExists, NotFount, User}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class UsersSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  private def mkUsers: Users[IO] = {
    val reqRes: ReqRes[IO] = (userId: Int) =>
      IO { if (userId == 2) ReqResUser("Bob", "Bobson").asRight else NotFount(s"$userId not found").asLeft }
    Users.impl[IO](InMemoryUsersRepo(), reqRes)
  }

  "Users" - {
    "should add new user" in {
      val users = mkUsers
      for {
        r <- users.add(2, "some@email.com")
      } yield r shouldBe Right(User("some@email.com", 2, "Bob", "Bobson"))
    }

    "should get user" in {
      val users = mkUsers
      for {
        _ <- users.add(2, "some@email.com")
        r <- users.get("some@email.com")
      } yield r shouldBe Right(User("some@email.com", 2, "Bob", "Bobson"))
    }

    "should remove user" in {
      val users = mkUsers
      for {
        _  <- users.add(2, "some@email.com")
        r1 <- users.remove("some@email.com")
        r2 <- users.get("some@email.com")
      } yield {
        r1 shouldBe Right(())
        r2 shouldBe Left(NotFount.byEmail("some@email.com"))
      }
    }

    "should not add same user twice" in {
      val users = mkUsers
      for {
        r1 <- users.add(2, "some@email.com")
        r2 <- users.add(2, "some@email.com")
      } yield {
        r1 shouldBe Right(User("some@email.com", 2, "Bob", "Bobson"))
        r2 should matchPattern { case Left(AlreadyExists(_)) => }
      }
    }

    "should not add unknown user" in {
      val users = mkUsers
      for {
        r <- users.add(1, "some@email.com")
      } yield {
        r should matchPattern { case Left(NotFount(_)) => }
      }
    }
  }
}
