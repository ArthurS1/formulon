package fr.konexii.formulon.infrastructure

import cats.effect._
import cats.effect.std._

import skunk._

import fr.konexii.formulon.application._
import fr.konexii.formulon.infrastructure._
import fr.konexii.formulon.application.Plugin

import natchez.Trace.Implicits.noop

final class PostgresRepositories[F[_]: Async: Console](
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String,
    plugins: List[Plugin]
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
    new postgres.SchemaAggregate(session, plugins)

  lazy val submission: repositories.SubmissionAggregate[F] =
    new postgres.SubmissionAggregate(session, plugins)
}
