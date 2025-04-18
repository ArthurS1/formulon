package fr.konexii.formulon.infrastructure.postgres

import skunk._
import skunk.data.Completion._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json.jsonb

import cats.effect._
import cats.syntax.all._
import cats.instances.all._

import java.util.UUID

import shapeless.HNil

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.presentation.Serialization._
import fr.konexii.formulon.application.Plugin

class SchemaAggregate[F[_]: Async](db: Resource[F, Session[F]], plugins: List[Plugin])
    extends repositories.SchemaAggregate[F] {

  private implicit val fieldDecoder: io.circe.Decoder[FieldWithMetadata] =
    decoderForFieldWithMetadata(plugins)

  private implicit val fieldEncoder: io.circe.Encoder[FieldWithMetadata] =
    encoderForFieldWithMetadata(plugins)

  private val chunkSize = 64

  private def schemaEntity(
      versions: List[Entity[Version]]
  ): Decoder[Entity[Blueprint]] =
    (uuid ~ varchar(80) ~ uuid.opt).emap { case id ~ name ~ activeId =>
      for {
        activeVersion <- activeId
          .map(activeId =>
            Either.fromOption(
              versions.find(e => e.id === activeId),
              s"Could not find the active schema version with id ${activeId.toString}."
            )
          )
          .sequence
      } yield Entity(id, Blueprint(name, versions, activeVersion))
    }

  lazy val versionEntity: Decoder[Entity[Version]] =
    (uuid ~ timestamp ~ uuid ~ jsonb[Tree[Entity[FieldWithMetadata]]])
      .map { case id ~ date ~ _ ~ content =>
        Entity(id, Version(date, content))
      }

  def delete(schema: Entity[Blueprint]): F[Unit] = db.use { s =>
    val schemaDeletion = sql"DELETE FROM schemas WHERE id = $uuid".command

    s.transaction.use(_ =>
      for {
        _ <- schema.data.versions.map(version => deleteVersion(version, s)).sequence_
        pc <- s.prepare(schemaDeletion)
        c <- pc.execute(schema.id)
        result <- c match {
          case Delete(count) if count < 1 =>
            Async[F].raiseError(
              new Exception(
                s"Could not delete schema with id ${schema.id}. (does the schema exist ?)"
              )
            )
          case _ => Async[F].unit
        }
      } yield result
    )
  }

  private def deleteVersion(version: Entity[Version], s: Session[F]): F[Unit] = {
    val versionDeletion =
      sql"DELETE FROM schema_versions WHERE schema_id = $uuid".command

    for {
      pc <- s.prepare(versionDeletion)
      c <- pc.execute(version.id)
      result <- c match {
        case Delete(count) if count < 1 =>
          Async[F].raiseError(
            new Exception(
              s"Could not delete schema version with id ${version.id}. (does the version exist ?)"
            )
          )
        case _ => Async[F].unit
      }
    } yield result
  }

  def create(schema: Entity[Blueprint]): F[Entity[Blueprint]] = db.use { s =>
    s.transaction.use(_ =>
      for {
        versions <- schema.data.versions.traverse(v =>
          createVersion(v, s, schema.id)
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

  def update(schema: Entity[Blueprint]): F[Entity[Blueprint]] = db.use { s =>
    val versionQuery =
      sql"SELECT id, date, schema_id, content FROM schema_versions WHERE schema_id = $uuid"
        .query(versionEntity)

    s.transaction.use(_ =>
      for {
        preparedVersionsQuery <- s.prepare(versionQuery)
        currentVersions <-
          preparedVersionsQuery
            .stream(schema.id, chunkSize)
            .compile
            .toList
        updatedVersions <- schema.data.versions.traverse(v =>
          currentVersions.find(_.id === v.id) match {
            case None          => createVersion(v, s, schema.id)
            case Some(version) => Async[F].pure(version)
          }
        )
        schemaQuery = sql"UPDATE schemas SET name = ${varchar(80)}, active_schema_id = ${uuid.opt} WHERE id = $uuid RETURNING *"
          .query(schemaEntity(updatedVersions))
        preparedSchemaQuery <- s.prepare(schemaQuery)
        schema <- preparedSchemaQuery.unique(
          schema.data.name *: schema.data.active.map(_.id) *: schema.id *: HNil
        )
      } yield schema
    )
  }

  private def createVersion(
      version: Entity[Version],
      s: Session[F],
      schemaId: UUID
  ): F[Entity[Version]] = {
    val versionInsertion =
      sql"INSERT INTO schema_versions VALUES ($uuid, $timestamp, $uuid, ${jsonb[Tree[
          Entity[FieldWithMetadata]
        ]]}) RETURNING *"
        .query(versionEntity)

    for {
      pc <- s.prepare(versionInsertion)
      result <- pc.unique(
        version.id *: version.data.date *: schemaId *: version.data.content *: HNil
      )
    } yield result
  }

  def get(id: UUID): F[Entity[Blueprint]] = db.use { s =>
    val versionQuery =
      sql"SELECT id, date, schema_id, content FROM schema_versions WHERE schema_id = $uuid"
        .query(versionEntity)

    s.transaction.use(_ =>
      for {
        preparedVersionsQuery <- s.prepare(versionQuery)
        versions <- preparedVersionsQuery
          .stream(id, chunkSize)
          .compile
          .toList
        schemaQuery =
          sql"SELECT id, name, active_schema_id FROM schemas WHERE id = $uuid"
            .query(schemaEntity(versions))
        preparedSchemaQuery <- s.prepare(schemaQuery)
        schema <- preparedSchemaQuery.unique(id)
      } yield schema
    )
  }

}
