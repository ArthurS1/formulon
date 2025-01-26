package fr.konexii.form
package application
package usecases

import java.util.UUID

import cats._
import cats.syntax.all._
import cats.effect._

import fr.konexii.form.domain._

class ReadVersion[F[_]: MonadThrow](respositories: Repositories[F]) {

  def execute(schemaId: String, versionId: String): F[Entity[SchemaVersion]] =
    for {
      schemaUuid <- MonadThrow[F].catchNonFatal(UUID.fromString(schemaId))
      versionUuid <- MonadThrow[F].catchNonFatal(UUID.fromString(versionId))
      schema <- respositories.schema.get(schemaUuid)
      result <- schema.data.versions.find(e => e.id === versionUuid) match {
        case None =>
          MonadThrow[F].raiseError(
            new Exception(s"Failed to find version with id $versionId")
          )
        case Some(value) => MonadThrow[F].pure(value)
      }
    } yield result

}
