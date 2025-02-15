package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std._

import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.ValidatorError._
import fr.konexii.form.presentation.Serialization._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F]
) {

  def execute(
      schemaId: String,
      versionId: String,
      rawSubmission: String
  ): F[Unit] = for {
    schemaUuid <- schemaId.toUuid
    versionUuid <- versionId.toUuid
    submissionUuid <- UUIDGen[F].randomUUID
    submission <- MonadThrow[F].fromEither(
      decode[Submission](rawSubmission)
        .map(Entity(submissionUuid, _))
    )
    schema <- repositories.schema.get(schemaUuid)
    version <- MonadThrow[F].fromOption(
      schema.data.versions.find(e => e.id === versionUuid),
      new Exception(s"Failed to find version $versionUuid in schema $schemaUuid.")
    )
    _ <- MonadThrow[F].fromValidated(
      Validator
        .validate(version.data.content, submission.data.answers)
        .leftMap(errors =>
          new Exception(errors.toList.map(_.show).mkString(", "))
        )
    )
    _ <- repositories.submission.create(submission, version)
  } yield ()

}
