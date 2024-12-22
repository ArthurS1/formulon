package fr.konexii.form
package infrastructure
package postgres

import cats.effect._
import cats._
import cats.implicits._
import java.time.LocalDateTime
import java.util.UUID
import slick.jdbc.PostgresProfile.api._

import fr.konexii.form.application._
import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema
import fr.konexii.form.domain.SchemaVersion
import fr.konexii.form.domain.End
import fr.konexii.form.domain.Block

class SchemaAggregate[F[_]](db: Database)(implicit
    F: Async[F]
) extends repositories.SchemaAggregate[F] {

  case class DataSchema(
      name: String,
      active: UUID
  )

  class Schemas(tag: Tag) extends Table[Entity[DataSchema]](tag, "schemas") {
    def id = column[UUID]("id", options = O.PrimaryKey)
    def name = column[String]("name")
    def active_schema_id = column[UUID]("active_schema_id")
    def * =
      (id, name, active_schema_id) <> ({ case (id, s, active) =>
        Entity(id, DataSchema(s, active))
      },
      (e: Entity[DataSchema]) => Some((e.id, e.data.name, e.data.active)))
  }

  case class DataSchemaVersion(
      date: LocalDateTime,
      schemaId: UUID
  )

  class SchemaVersions(tag: Tag)
      extends Table[Entity[DataSchemaVersion]](tag, "schema_versions") {
    def id = column[UUID]("id", options = O.PrimaryKey)
    def date = column[LocalDateTime]("date")
    def schema_id = column[UUID]("schema_id")
    def * = (id, date, schema_id) <> ({ case (id, date, schema_id) =>
      Entity(id, DataSchemaVersion(date, schema_id))
    },
    (e: Entity[DataSchemaVersion]) =>
      Some((e.id, e.data.date, e.data.schemaId)))
  }

  lazy val schemas = TableQuery[Schemas]
  lazy val schemaVersions = TableQuery[SchemaVersions]

  /* Will be moved */

  def delayQuery[A](a: DBIOAction[A, NoStream, Nothing]): F[A] =
    F.fromFuture(
      F.delay(
        db.run(
          a
        )
      )
    )

  def get(id: String): F[Entity[domain.Schema]] =
    for {
      schema <- delayQuery(
        schemas.filter(_.id === UUID.fromString(id)).result.head
      )
      activeSchema <- delayQuery(
        schemaVersions.filter(_.id === schema.data.active).result.head
      )
      versions <- delayQuery(
        schemaVersions.filter(_.schema_id === schema.id).result
      )
      schemaVersions = versions.map { case Entity(id, data) =>
        Entity(id, domain.SchemaVersion(data.date, ???))
      }
      activeSchemaVersion = Entity(
        activeSchema.id,
        domain.SchemaVersion(activeSchema.data.date, ???)
      )
      result = Entity(
        schema.id,
        domain.Schema(
          name = schema.data.name,
          versions = schemaVersions.toList,
          active = Some(activeSchemaVersion)
        )
      )
    } yield result

  def save(schema: Entity[domain.Schema]): F[Entity[domain.Schema]] =
    for {
      _ <- delayQuery(
        schemas
          .+=(
            Entity(
              schema.id,
              DataSchema(
                name = schema.data.name,
                active = schema.data.active.get.id
              )
            )
          )
          .void
      )
      _ <- schema.data.versions
        .map((version: domain.Entity[domain.SchemaVersion]) =>
          delayQuery(
            schemaVersions
              .+=(
                Entity(
                  version.id,
                  DataSchemaVersion(version.data.date, schema.id)
                )
              )
              .void
          )
        )
        .sequence
    } yield schema

  def delete(id: String): F[Unit] =
    delayQuery(schemas.filter(_.id === UUID.fromString(id)).delete.void) >>
      delayQuery(
        schemaVersions.filter(_.schema_id === UUID.fromString(id)).delete.void
      )

  def update(schema: Entity[Schema]): F[Entity[Schema]] =
    delayQuery(
      schemas
        .filter(_.id === schema.id)
        .update(Entity(schema.id, DataSchema(schema.data.name, schema.data.active.get.id)))
        .void
      ) >> F.pure(schema)

}
