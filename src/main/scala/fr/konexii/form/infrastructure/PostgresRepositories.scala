package fr.konexii.form
package infrastructure

import slick.jdbc.PostgresProfile.api._
import cats.effect._

import fr.konexii.form._

final class PostgresRepositories[F[_]](
    jdbcUrl: String,
    user: String,
    password: String
)(implicit F: Async[F])
    extends application.Repositories[F] {

  /* TODO : Sometime, check if making all repositories lazy improves performances ? */

  lazy val db =
    Database.forURL(jdbcUrl, user, password, driver = "org.postgresql.Driver")

  lazy val schema: application.repositories.SchemaAggregate[F] =
    new infrastructure.postgres.SchemaAggregate(db)
}
