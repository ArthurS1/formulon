package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.Validation._
import fr.konexii.formulon.presentation.Exceptions._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F],
    plugins: List[Plugin]
) {

  def execute(
      blueprintId: UUID,
      versionId: UUID,
      submission: Submission
  ): F[Unit] = for {
    uuid <- UUIDGen[F].randomUUID
    submissionEntity = Entity(uuid, submission)
    blueprint <- repositories.blueprint.get(blueprintId)
    version <- MonadThrow[F].fromOption(
      blueprint.data.versions.find(e => e.id === versionId),
      new Exception(s"Failed to find version $versionId in schema $blueprint.")
    )
    _ <- MonadThrow[F].fromEither(
      Validator
        .validate(
          version.data.content,
          submissionEntity.data,
          validateWrapper(plugins)
        )
        .leftMap(errors => CompositeException(errors.map(_.show)))
    )
    _ <- repositories.submission.create(submissionEntity, version)
  } yield ()

}
