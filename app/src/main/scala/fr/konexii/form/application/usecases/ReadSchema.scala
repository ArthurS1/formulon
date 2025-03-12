package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories

class ReadSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: UUID): F[Entity[Schema]] =
    for {
      result <- repositories.schema.get(id)
    } yield result

}
