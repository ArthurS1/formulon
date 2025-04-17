package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std._

import java.util.UUID

import io.circe.parser.decode

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories
import fr.konexii.formulon.presentation.Serialization._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F]
) {

  def execute(
      schemaId: UUID,
      versionId: UUID,
      rawSubmission: String
  ): F[Unit] = MonadThrow[F].unit/*for {
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
  } yield ()*/

}
