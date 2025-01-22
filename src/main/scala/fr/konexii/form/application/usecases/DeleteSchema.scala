package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.application.Repositories

class DeleteSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Unit] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      result <- repositories.schema.delete(uuid)
    } yield result

}
