package com.kambius.simpleapp.repos

import cats.effect.IO
import com.kambius.simpleapp.services.Users.User
import doobie.syntax.connectionio._
import doobie.util.fragment.Fragment
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class UsersRepoSpec extends AsyncFreeSpec with Matchers with EmbeddedPostgresSpec {
  private var repo: UsersRepo[IO] = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    Fragment
      .const(Source.fromFile("sql/schema.sql").mkString)
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()
    repo = UsersRepo.impl(transactor)
  }

  "UsersRepo" - {
    val user = User("some@email.com", 42, "Bob", "Bobson")

    "save" in {
      repo.add(user).map(_ shouldBe true)
    }

    "retrieve user" in {
      repo.get(user.email).map(_ should contain(user))
    }

    "not save the same user twice" in {
      repo.add(user).map(_ shouldBe false)
    }

    "remove user" in {
      repo.remove(user.email).map(_ shouldBe true)
    }

    "not retrieve deleted user" in {
      repo.get(user.email).map(_ shouldBe None)
    }

    "not remove the same user twice" in {
      repo.remove(user.email).map(_ shouldBe false)
    }
  }
}
