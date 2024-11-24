package fr.konexii.form
package infrastructure
package repositories

import cats.effect._
import cats._
import cats.implicits._
import java.util.UUID
import slick.jdbc.PostgresProfile.api._

import fr.konexii.form.application._

class SchemaPostgres[F[_]](db: Database)(implicit
    F: Async[F]
) extends repositories.Schema[F] {

  class Schemas(tag: Tag) extends Table[domain.Schema](tag, "schemas") {
    def id = column[UUID]("id", options = O.PrimaryKey)
    def name = column[String]("name")
    def * = (id, name).<>(domain.Schema.tupled, domain.Schema.unapply)
  }

  val schemas = TableQuery[Schemas]

  def get(id: String): F[Option[domain.Schema]] =
    F.fromFuture(
      F.delay(
        db.run(
          schemas.filter(_.id === UUID.fromString(id)).result.headOption
        )
      )
    )

  def save(schema: domain.Schema): F[domain.Schema] =
    F.fromFuture(
      (
        F.delay(
          db.run(
            schemas.+=(schema).void
          )
        )
      )
    ) >> F.pure(schema)
}
