package fr.konexii.form
package infrastructure

import cats.effect._
import cats.effect.std._

import skunk._

import natchez.Trace.Implicits.noop

final class PostgresRepositories[F[_]: Async : Console](
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
)
    extends application.Repositories[F] {

  lazy val session: Resource[F, Session[F]] =
    Session.single(
      host = host,
      port = port,
      user = user,
      database = database,
      password = Some(password)
    )

  lazy val schema: application.repositories.SchemaAggregate[F] =
    new infrastructure.postgres.SchemaAggregate(session)
}
