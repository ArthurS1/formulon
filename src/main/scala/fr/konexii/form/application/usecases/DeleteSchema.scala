package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class DeleteSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Unit] =
    for {
      uuid <- id.toUuid
      result <- repositories.schema.delete(uuid)
    } yield result

}
