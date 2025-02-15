package fr.konexii.form.infrastructure

import cats.effect._
import cats.effect.std._

import skunk._

import fr.konexii.form.application._
import fr.konexii.form.infrastructure._

import natchez.Trace.Implicits.noop

final class PostgresRepositories[F[_]: Async: Console](
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
) extends Repositories[F] {

  import fs2.io.net.Network

  // Passing an Async instance directly to single is deprecated.
  implicit val networkInstance: Network[F] = Network.forAsync(Async[F])

  lazy val session: Resource[F, Session[F]] =
    Session.single(
      host = host,
      port = port,
      user = user,
      database = database,
      password = Some(password)
    )

  lazy val schema: repositories.SchemaAggregate[F] =
    new postgres.SchemaAggregate(session)

  lazy val submission: repositories.SubmissionAggregate[F] =
    new postgres.SubmissionAggregate(session)
}
