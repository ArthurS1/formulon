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

class BlueprintAggregate[F[_]: Async](
    db: Resource[F, Session[F]],
    plugins: List[Plugin]
) extends repositories.BlueprintAggregate[F] {

  implicit val answerDecoder: io.circe.Decoder[Answer] = decoderForAnswer(
    plugins
  )

  implicit val answerEncoder: io.circe.Encoder[Answer] = encoderForAnswer(
    plugins
  )

  implicit val fieldDecoder: io.circe.Decoder[FieldWithMetadata] =
    decoderForFieldWithMetadata(plugins)

  implicit val fieldEncoder: io.circe.Encoder[FieldWithMetadata] =
    encoderForFieldWithMetadata(plugins)

  private val chunkSize = 64

  private def blueprintEntity(
      versions: List[Entity[Version]]
  ): Decoder[Entity[Blueprint]] =
    (uuid ~ varchar(80) ~ uuid.opt ~ varchar(80)).emap { case id ~ name ~ activeId ~ orgName =>
      for {
        activeVersion <- activeId
          .map(activeId =>
            Either.fromOption(
              versions.find(e => e.id === activeId),
              s"Could not find the active blueprint version with id ${activeId.toString}."
            )
          )
          .sequence
      } yield Entity(id, Blueprint(name, orgName, versions, activeVersion))
    }

  lazy val versionEntity: Decoder[Entity[Version]] =
    (uuid ~ timestamp ~ uuid ~ jsonb[Tree[Entity[FieldWithMetadata]]])
      .map { case id ~ date ~ _ ~ content =>
        Entity(id, Version(date, content))
      }

  def delete(blueprint: Entity[Blueprint]): F[Unit] = db.use { s =>
    val blueprintDeletion = sql"DELETE FROM schemas WHERE id = $uuid".command

    s.transaction.use(_ =>
      for {
        _ <- blueprint.data.versions
          .map(version => deleteVersion(version, s))
          .sequence_
        pc <- s.prepare(blueprintDeletion)
        c <- pc.execute(blueprint.id)
        result <- c match {
          case Delete(count) if count < 1 =>
            Async[F].raiseError(
              new Exception(
                s"Could not delete schema with id ${blueprint.id}. (does the schema exist ?)"
              )
            )
          case _ => Async[F].unit
        }
      } yield result
    )
  }

  private def deleteVersion(
      version: Entity[Version],
      s: Session[F]
  ): F[Unit] = {
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

  def create(blueprint: Entity[Blueprint]): F[Entity[Blueprint]] = db.use { s =>
    s.transaction.use(_ =>
      for {
        versions <- blueprint.data.versions.traverse(v =>
          createVersion(v, s, blueprint.id)
        )
        blueprintInsertion =
          sql"INSERT INTO schemas VALUES ($uuid, $varchar, ${uuid.opt}) RETURNING *"
            .query(blueprintEntity(versions))
        pc <- s.prepare(blueprintInsertion)
        result <- pc.unique(
          blueprint.id :: blueprint.data.name :: blueprint.data.active
            .map(_.id) :: HNil
        )
      } yield result
    )
  }

  def update(blueprint: Entity[Blueprint]): F[Entity[Blueprint]] = db.use { s =>
    val versionQuery =
      sql"SELECT id, date, schema_id, content FROM schema_versions WHERE schema_id = $uuid"
        .query(versionEntity)

    s.transaction.use(_ =>
      for {
        preparedVersionsQuery <- s.prepare(versionQuery)
        currentVersions <-
          preparedVersionsQuery
            .stream(blueprint.id, chunkSize)
            .compile
            .toList
        updatedVersions <- blueprint.data.versions.traverse(v =>
          currentVersions.find(_.id === v.id) match {
            case None          => createVersion(v, s, blueprint.id)
            case Some(version) => Async[F].pure(version)
          }
        )
        blueprintQuery = sql"UPDATE schemas SET name = ${varchar(80)}, active_schema_id = ${uuid.opt} WHERE id = $uuid RETURNING *"
          .query(blueprintEntity(updatedVersions))
        preparedBlueprintQuery <- s.prepare(blueprintQuery)
        blueprint <- preparedBlueprintQuery.unique(
          blueprint.data.name *: blueprint.data.active.map(_.id) *: blueprint.id *: HNil
        )
      } yield blueprint
    )
  }

  private def createVersion(
      version: Entity[Version],
      s: Session[F],
      blueprintId: UUID
  ): F[Entity[Version]] = {
    val versionInsertion =
      sql"INSERT INTO schema_versions VALUES ($uuid, $timestamp, $uuid, ${jsonb[Tree[
          Entity[FieldWithMetadata]
        ]]}) RETURNING *"
        .query(versionEntity)

    for {
      pc <- s.prepare(versionInsertion)
      result <- pc.unique(
        version.id *: version.data.date *: blueprintId *: version.data.content *: HNil
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
        blueprintQuery =
          sql"SELECT id, name, active_schema_id FROM schemas WHERE id = $uuid"
            .query(blueprintEntity(versions))
        preparedBlueprintQuery <- s.prepare(blueprintQuery)
        blueprint <- preparedBlueprintQuery.unique(id)
      } yield blueprint
    )
  }

}
