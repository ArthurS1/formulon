package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.application.Repositories

class DeleteSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: UUID): F[Unit] =
    for {
      schema <- repositories.schema.get(id)
      result <- repositories.schema.delete(schema)
    } yield result

}
