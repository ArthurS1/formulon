package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

class UpdateSchema[F[_]: MonadThrow](repositories: Repositories[F]) {
  def execute(
      update: dtos.UpdateSchemaRequest,
      id: String
  ): F[Entity[domain.Schema]] = {
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      schemaToUpdate <- repositories.schema.get(uuid)
      updatedSchema <- repositories.schema.update(
        schemaToUpdate.copy(data =
          schemaToUpdate.data.copy(name = update.name)
        )
      )
    } yield updatedSchema
  }
}
