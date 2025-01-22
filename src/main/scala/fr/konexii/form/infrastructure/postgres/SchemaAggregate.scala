package fr.konexii.form
package infrastructure
package postgres

import skunk._
import skunk.data.Completion._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json.jsonb

import fs2.Stream

import cats.effect._
import cats.effect.std._
import cats._
import cats.syntax.all._
import cats.instances.all._

import io.circe.parser.decode
import io.circe.syntax._

import java.time.LocalDateTime
import java.util.UUID

import shapeless.HNil
import skunk.data.Completion

import fr.konexii.form.application._
import fr.konexii.form.domain._
import fr.konexii.form.presentation.JsonUtils._

class SchemaAggregate[F[_]: Async: Console](db: Resource[F, Session[F]])
    extends repositories.SchemaAggregate[F] {

  private def schemaEntity(
      versions: List[Entity[SchemaVersion]]
  ): Decoder[Entity[Schema]] =
    (uuid ~ varchar(80) ~ uuid.opt).emap { case id ~ name ~ activeId =>
      for {
        activeVersion <- activeId
          .map(activeId =>
            Either.fromOption(
              versions.find(e => e.id === activeId),
              s"Could not find the active schema version with id ${activeId.toString}"
            )
          )
          .sequence
      } yield Entity(id, Schema(name, versions, activeVersion))
    }

  lazy val versionEntity: Decoder[Entity[SchemaVersion]] =
    (uuid ~ timestamp ~ uuid ~ jsonb[SchemaTree[FieldWithMetadata]]).map {
      case id ~ date ~ schemaId ~ content =>
        Entity(id, SchemaVersion(date, content))
    }

  def delete(id: UUID): F[Unit] = db.use { s =>
    val schemaDeletion = sql"DELETE FROM schemas WHERE id = $uuid".command
    for {
      pc <- s.prepare(schemaDeletion)
      c <- pc.execute(id)
      result <- c match {
        case Delete(count) if count < 1 =>
          Async[F].raiseError(
            new Exception(
              s"Could not delete schema with id $id (does the schema exist ?)"
            )
          )
        case _ => Async[F].unit
      }
    } yield result
  }

  private def deleteVersion(id: UUID, s: Session[F]): F[Unit] = {
    val versionDeletion =
      sql"DELETE FROM schema_versions WHERE schema_id = $uuid".command

    for {
      pc <- s.prepare(versionDeletion)
      c <- pc.execute(id)
      result <- c match {
        case Delete(count) if count < 1 =>
          Async[F].raiseError(
            new Exception(
              s"Could not delete schema version with id $id (does the version exist ?)"
            )
          )
        case _ => Async[F].unit
      }
    } yield result
  }

  def save(schema: Entity[Schema]): F[Entity[Schema]] = db.use { s =>
    s.transaction.use(f =>
      for {
        versions <- schema.data.versions.traverse(v =>
          saveVersion(v, s, schema.id)
        )
        schemaInsertion =
          sql"INSERT INTO schemas VALUES ($uuid, $varchar, ${uuid.opt}) RETURNING *"
            .query(schemaEntity(versions))

        pc <- s.prepare(schemaInsertion)
        result <- pc.unique(
          schema.id :: schema.data.name :: schema.data.active
            .map(_.id) :: HNil
        )
      } yield result
    )
  }

  private def saveVersion(
      version: Entity[SchemaVersion],
      s: Session[F],
      schemaId: UUID
  ): F[Entity[SchemaVersion]] = {
    val versionInsertion =
      sql"INSERT INTO schema_versions VALUES ($uuid, $timestamp, $uuid) RETURNING *"
        .query(versionEntity)

    for {
      pc <- s.prepare(versionInsertion)
      result <- pc.unique(version.id *: version.data.date *: schemaId *: HNil)
    } yield result
  }

  def update(schema: Entity[Schema]): F[Entity[Schema]] = db.use { s =>
    val versionQuery =
      sql"""
          INSERT INTO schema_versions (id, date, schema_id, content)
          VALUES ($uuid, $timestamp, $uuid, ${jsonb[SchemaTree[
          FieldWithMetadata
        ]]})
          ON CONFLICT (id) DO UPDATE SET content = ${jsonb[SchemaTree[
          FieldWithMetadata
        ]]}
          RETURNING *
          """
        .query(versionEntity)

    s.transaction.use(f =>
      for {
        preparedVersionQuery <- s.prepare(versionQuery)
        versions <- schema.data.versions.traverse(v =>
          preparedVersionQuery.unique(
            v.id *: v.data.date *: schema.id *: v.data.content *: v.data.content *: HNil
          )
        )
        schemaQuery = sql"UPDATE schemas SET name = ${varchar(80)}, active_schema_id = ${uuid.opt} WHERE id = $uuid RETURNING *"
          .query(schemaEntity(versions))
        preparedSchemaQuery <- s.prepare(schemaQuery)
        schema <- preparedSchemaQuery.unique(
          schema.data.name *: schema.data.active.map(_.id) *: schema.id *: HNil
        )
      } yield schema
    )
  }

  def get(id: UUID): F[Entity[Schema]] = db.use { s =>
    val versionQuery =
      sql"SELECT id, date, schema_id, content FROM schema_versions WHERE schema_id = $uuid"
        .query(versionEntity)

    s.transaction.use(f =>
      for {
        preparedVersionsQuery <- s.prepare(versionQuery)
        versions <- preparedVersionsQuery
          .stream(id, 64)
          .compile
          .toList // TODO : understand how chunk size matters and should be set
        schemaQuery =
          sql"SELECT id, name, active_schema_id FROM schemas WHERE id = $uuid"
            .query(schemaEntity(versions))
        preparedSchemaQuery <- s.prepare(schemaQuery)
        schema <- preparedSchemaQuery.unique(id)
      } yield schema
    )
  }

}
