package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories

class ReadVersionList[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      id: UUID
  ): F[List[Entity[SchemaVersion]]] =
    for {
      schema <- repositories.schema.get(id)
    } yield schema.data.versions

}
