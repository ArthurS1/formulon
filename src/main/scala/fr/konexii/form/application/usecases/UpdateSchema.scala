package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories
import fr.konexii.form.application.dtos.UpdateSchemaRequest
import fr.konexii.form.application.utils.uuid._

class UpdateSchema[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      update: UpdateSchemaRequest,
      id: String
  ): F[Entity[Schema]] = {
    for {
      uuid <- id.toUuid
      schemaToUpdate <- repositories.schema.get(uuid)
      updatedSchema <- repositories.schema.update(
        schemaToUpdate.copy(data =
          schemaToUpdate.data.copy(name = update.name)
        )
      )
    } yield updatedSchema
  }

}
