package fr.konexii.form
package infrastructure
package repositories

import cats.effect._
import cats._
import cats.implicits._
import java.util.UUID
import slick.jdbc.PostgresProfile.api._

import fr.konexii.form.application._
import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

class SchemaPostgres[F[_]](db: Database)(implicit
    F: Async[F]
) extends repositories.Schema[F] {

  class Schemas(tag: Tag) extends Table[Entity[Schema]](tag, "schemas") {
    def id = column[UUID]("id", options = O.PrimaryKey)
    def name = column[String]("name")
    def * =
      (id, name) <> ({ case (id, s) => Entity(id, Schema(s)) },
      (e: Entity[Schema]) => Some((e.id, e.data.name)))
  }

  lazy val schemas = TableQuery[Schemas]

  def get(id: String): F[Option[Entity[Schema]]] =
    F.fromFuture(
      F.delay(
        db.run(
          schemas.filter(_.id === UUID.fromString(id)).result.headOption
        )
      )
    )

  def save(schema: Entity[Schema]): F[Entity[Schema]] =
    F.fromFuture(
      F.delay(
        db.run(
          schemas.+=(schema).void
        )
      )
    ) >> F.pure(schema)

  def delete(id: String): F[Unit] =
    F.fromFuture(
      F.delay(
        db.run(
          schemas.filter(_.id === UUID.fromString(id)).delete.void
        )
      )
    )

  def update(schema: Entity[Schema]): F[Entity[Schema]] =
    F.fromFuture(
      F.delay(
        db.run(
          schemas.filter(_.id === schema.id).update(schema)
        )
      )
    ) >> F.pure(schema)

}
