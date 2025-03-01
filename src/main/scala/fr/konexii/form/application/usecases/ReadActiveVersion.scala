package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories

class ReadActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: UUID): F[Entity[SchemaVersion]] =
    for {
      schema <- repositories.schema.get(id)
      activeVersion <- MonadThrow[F].fromOption(
        schema.data.active,
        new Exception(s"No active version for schema with id $id.")
      )
    } yield activeVersion

}
