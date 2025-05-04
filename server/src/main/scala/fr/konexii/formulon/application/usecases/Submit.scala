package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std._

import java.util.UUID

import io.circe._
import io.circe.parser.decode

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.Validation._
import fr.konexii.formulon.presentation.Serialization._
import fr.konexii.formulon.presentation.ValidatorExceptionInstances._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F],
    plugins: List[Plugin]
) {

  implicit val decoder: Decoder[Answer] = decoderForAnswer(plugins)

  def execute(
      schemaId: UUID,
      versionId: UUID,
      rawSubmission: String
  ): F[Unit] = for {
    uuid <- UUIDGen[F].randomUUID
    submission <- MonadThrow[F].fromEither(
      decode[Submission](rawSubmission)
        .map(Entity(uuid, _))
    )
    blueprint <- repositories.blueprint.get(schemaId)
    version <- MonadThrow[F].fromOption(
      blueprint.data.versions.find(e => e.id === versionId),
      new Exception(s"Failed to find version $versionId in schema $schemaId.")
    )
    _ <- MonadThrow[F].fromEither(
      Validator
        .validate(
          version.data.content,
          submission.data,
          validateWrapper(plugins)
        )
        .leftMap(errors =>
          CompositeException(errors.map(Show[ValidatorException].show(_)))
        )
    )
    _ <- repositories.submission.create(submission, version)
  } yield ()

}
