package fr.konexii.formulon.infrastructure.postgres

import cats.effect._
import cats.syntax.all._

import shapeless.HNil

import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json.jsonb

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.application.repositories
import fr.konexii.formulon.presentation.Serialization._

import io.circe.Encoder._

class SubmissionAggregate[F[_]: Async](db: Resource[F, Session[F]], plugins: List[Plugin])
    extends repositories.SubmissionAggregate[F] {

  private val chunkSize = 64

  implicit val decoder: io.circe.Decoder[Answer] = decoderForAnswer(plugins)
  implicit val encoder: io.circe.Encoder[List[Entity[Answer]]] = encodeList(encoderForEntity(encoderForAnswer(plugins)))

  lazy val submissionEntity: Decoder[Entity[Submission]] = {
    (uuid ~ uuid ~ jsonb[List[Entity[Answer]]]).map {
      case id ~ _ ~ answers => Entity(id, Submission(answers))
    }
  }

  def create(
      submission: Entity[Submission],
      version: Entity[Version]
  ): F[Entity[Submission]] = db.use { s =>
    val submissionInsertion =
      sql"INSERT INTO answers VALUES ($uuid, $uuid, ${jsonb[List[Entity[Answer]]]}) RETURNING *"
        .query(submissionEntity)

    s.transaction.use(_ =>
      for {
        pc <- s.prepare(submissionInsertion)
        result <- pc.unique(
          submission.id *: version.id *: submission.data.answers *: HNil
        )
      } yield result
    )
  }

  def getAll(version: Entity[Version]): F[List[Entity[Submission]]] =
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
