package fr.konexii.form.infrastructure.postgres

import cats.effect._
import cats.syntax.all._

import shapeless.HNil

import io.circe.syntax._

import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json.jsonb

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.application.repositories
import fr.konexii.form.presentation.Serialization._

class SubmissionAggregate[F[_]: Async](db: Resource[F, Session[F]])
    extends repositories.SubmissionAggregate[F] {

  private val chunkSize = 64

  lazy val submissionEntity: Decoder[Entity[Submission]] = {
    (uuid ~ uuid ~ jsonb[List[Entity[Answer]]]).map {
      case id ~ versionId ~ answers => Entity(id, Submission(answers))
    }
  }

  def create(
      submission: Entity[Submission],
      version: Entity[SchemaVersion]
  ): F[Entity[Submission]] = db.use { s =>
    val submissionInsertion =
      sql"INSERT INTO answers VALUES ($uuid, $uuid, ${jsonb[List[Entity[Answer]]]}) RETURNING *"
        .query(submissionEntity)

    s.transaction.use(f =>
      for {
        pc <- s.prepare(submissionInsertion)
        result <- pc.unique(
          submission.id *: version.id *: submission.data.answers *: HNil
        )
      } yield result
    )
  }

  def getAll(version: Entity[SchemaVersion]): F[List[Entity[Submission]]] =
    db.use { s =>
      val answersQuery =
        sql"SELECT * FROM answers WHERE schema_version_id = $uuid".query(
          submissionEntity
        )

      for {
        preparedAnswersQuery <- s.prepare(answersQuery)
        answers <- preparedAnswersQuery
          .stream(version.id, chunkSize)
          .compile
          .toList
      } yield answers
    }

}
