package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.domain._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class ReadActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: String): F[Entity[SchemaVersion]] =
    for {
      uuid <- id.toUuid
      schema <- repositories.schema.get(uuid)
      activeVersion <- MonadThrow[F].fromOption(
        schema.data.active,
        new Exception(s"No active version for schema with id $id.")
      )
    } yield activeVersion

}
