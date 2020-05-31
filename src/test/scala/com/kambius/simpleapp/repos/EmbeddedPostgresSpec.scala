package com.kambius.simpleapp.repos

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import doobie.util.transactor.Transactor
import org.scalatest.BeforeAndAfterAll

trait EmbeddedPostgresSpec extends AsyncIOSpec with BeforeAndAfterAll {
  private var postgres: EmbeddedPostgres = _
  protected var transactor: Transactor[IO] = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    postgres = EmbeddedPostgres.builder().start()
    transactor = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      postgres.getJdbcUrl("postgres", "postgres"),
      "postgres",
      "postgres"
    )
  }

  override protected def afterAll(): Unit = {
    postgres.close()
    super.afterAll()
  }
}
