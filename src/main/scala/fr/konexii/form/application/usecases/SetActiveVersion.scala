package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._

class SetActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(schemaId: String, versionId: String): F[Unit] =
    for {
      schemaUuid <- MonadThrow[F].catchNonFatal(UUID.fromString(schemaId))
      versionUuid <- MonadThrow[F].catchNonFatal(UUID.fromString(versionId))
      schema <- repositories.schema.get(schemaUuid)
      version <- schema.data.versions.find(_.id === versionUuid) match {
        case None =>
          MonadThrow[F].raiseError(
            new Exception(s"Could not find version $versionId")
          )
        case Some(value) => MonadThrow[F].pure(value)
      }
      _ <- repositories.schema.update(
        schema.copy(data = schema.data.copy(active = Some(version)))
      )
    } yield ()

}
