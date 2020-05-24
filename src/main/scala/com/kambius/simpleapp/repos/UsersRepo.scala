package com.kambius.simpleapp.repos

import cats.effect.Sync
import cats.syntax.functor._
import com.kambius.simpleapp.services.Users.User
import doobie.postgres.sqlstate
import doobie.syntax.applicativeerror._
import doobie.syntax.connectionio._
import doobie.syntax.string._
import doobie.util.transactor.Transactor

trait UsersRepo[F[_]] {
  def add(user: User): F[Boolean]
  def get(email: String): F[Option[User]]
  def remove(email: String): F[Boolean]
}

object UsersRepo {
  def impl[F[_]: Sync](tx: Transactor[F]): UsersRepo[F] = new UsersRepoImpl[F](tx)
}

class UsersRepoImpl[F[_]: Sync](tx: Transactor[F]) extends UsersRepo[F] {
  override def add(user: User): F[Boolean] = {
    val query =
      sql"""INSERT INTO users (email, id, first_name, last_name)
           |VALUES (${user.email}, ${user.id}, ${user.firstName}, ${user.lastName})
         """.stripMargin
    query.update.run
      .transact(tx)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION => () }
      .map(_.isRight)
  }

  override def get(email: String): F[Option[User]] = {
    val query =
      sql"""SELECT email, id, first_name, last_name
           |FROM users
           |WHERE email = $email
         """.stripMargin
    query
      .query[User]
      .option
      .transact(tx)
  }

  override def remove(email: String): F[Boolean] = {
    val query =
      sql"""DELETE FROM users
           |WHERE email = $email
         """.stripMargin
    query.update.run
      .transact(tx)
      .map(_ > 0)
  }
}
