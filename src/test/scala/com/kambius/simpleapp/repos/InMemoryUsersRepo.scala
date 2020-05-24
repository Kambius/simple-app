package com.kambius.simpleapp.repos

import cats.effect.IO
import com.kambius.simpleapp.services.Users.User

final case class InMemoryUsersRepo(var users: Map[String, User] = Map.empty) extends UsersRepo[IO] {
  override def add(user: User): IO[Boolean] = IO {
    if (!users.contains(user.email)) {
      users += user.email -> user
      true
    } else {
      false
    }
  }

  override def get(email: String): IO[Option[User]] = IO {
    users.get(email)
  }

  override def remove(email: String): IO[Boolean] = IO {
    if (users.contains(email)) {
      users -= email
      true
    } else {
      false
    }
  }
}
