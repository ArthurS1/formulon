package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories
import fr.konexii.formulon.application.dtos.UpdateSchemaRequest

class UpdateSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      update: UpdateSchemaRequest,
      id: UUID
  ): F[Entity[Blueprint]] = {
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
