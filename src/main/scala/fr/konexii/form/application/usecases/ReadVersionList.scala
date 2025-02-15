package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.domain._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class ReadVersionList[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      id: String
  ): F[List[Entity[SchemaVersion]]] =
    for {
      uuid <- id.toUuid
      schema <- repositories.schema.get(uuid)
    } yield schema.data.versions

}
