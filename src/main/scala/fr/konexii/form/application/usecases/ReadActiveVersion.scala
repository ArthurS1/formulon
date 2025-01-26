package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._

class ReadActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Entity[SchemaVersion]] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      schema <- repositories.schema.get(uuid)
      activeVersion <- schema.data.active match {
        case None =>
          MonadThrow[F].raiseError(
            new Exception(s"No active version for the schema with id $id")
          )
        case Some(value) => MonadThrow[F].pure(value)
      }
    } yield activeVersion

}
