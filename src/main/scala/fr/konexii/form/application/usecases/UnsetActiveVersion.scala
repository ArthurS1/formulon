package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class UnsetActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Unit] =
    for {
      uuid <- id.toUuid
      schema <- repositories.schema.get(uuid)
      _ <- repositories.schema.update(
        schema.copy(data = schema.data.copy(active = None))
      )
    } yield ()

}
