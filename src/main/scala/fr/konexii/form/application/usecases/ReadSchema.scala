package fr.konexii.form
package application
package usecases

import cats.implicits._
import cats._

import java.util.UUID

import fr.konexii.form.application.Repositories
import fr.konexii.form.domain._

class ReadSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Entity[Schema]] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      result <- repositories.schema.get(uuid)
    } yield result

}
