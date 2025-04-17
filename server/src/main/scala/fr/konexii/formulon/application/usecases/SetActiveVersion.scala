package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.application.Repositories

class SetActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(schemaId: UUID, versionId: UUID): F[Unit] =
    for {
      schema <- repositories.schema.get(schemaId)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(_.id === versionId),
        new Exception(s"Could not find version $versionId.")
      )
      _ <- repositories.schema.update(
        schema.copy(data = schema.data.copy(active = Some(version)))
      )
    } yield ()

}
