package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories
import fr.konexii.form.application.dtos.UpdateSchemaRequest

class UpdateSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      update: UpdateSchemaRequest,
      id: UUID
  ): F[Entity[Schema]] = {
    for {
      schemaToUpdate <- repositories.schema.get(id)
      updatedSchema <- repositories.schema.update(
        schemaToUpdate.copy(data =
          schemaToUpdate.data.copy(name = update.name)
        )
      )
    } yield updatedSchema
  }

}
