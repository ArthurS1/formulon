package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class SetActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(schemaId: String, versionId: String): F[Unit] =
    for {
      schemaUuid <- schemaId.toUuid
      versionUuid <- versionId.toUuid
      schema <- repositories.schema.get(schemaUuid)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(_.id === versionUuid),
        new Exception(s"Could not find version $versionId.")
      )
      _ <- repositories.schema.update(
        schema.copy(data = schema.data.copy(active = Some(version)))
      )
    } yield ()

}
