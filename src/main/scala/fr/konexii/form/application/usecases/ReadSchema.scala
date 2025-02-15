package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories

class ReadSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Entity[Schema]] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      result <- repositories.schema.get(uuid)
    } yield result

}
