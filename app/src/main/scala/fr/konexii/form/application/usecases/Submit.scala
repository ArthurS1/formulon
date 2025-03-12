package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std._

import java.util.UUID

import io.circe.parser.decode

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.ValidatorError._
import fr.konexii.form.presentation.Serialization._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F]
) {

  def execute(
      schemaId: UUID,
      versionId: UUID,
      rawSubmission: String
  ): F[Unit] = for {
    submissionUuid <- UUIDGen[F].randomUUID
    submission <- MonadThrow[F].fromEither(
      decode[Submission](rawSubmission)
        .map(Entity(submissionUuid, _))
    )
    schema <- repositories.schema.get(schemaId)
    version <- MonadThrow[F].fromOption(
      schema.data.versions.find(e => e.id === versionId),
      new Exception(s"Failed to find version $versionId in schema $schemaId.")
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
