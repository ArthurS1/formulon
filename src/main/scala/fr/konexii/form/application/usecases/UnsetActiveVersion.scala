package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

class UnsetActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Unit] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      schema <- repositories.schema.get(uuid)
      _ <- repositories.schema.update(
        schema.copy(data = schema.data.copy(active = None))
      )
    } yield ()

}
