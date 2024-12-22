package fr.konexii.form
package application
package usecases

import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema
import cats.instances.list

class UpdateSchema[F[_]](repositories: Repositories[F]) {
  def execute(
      update: Entity[dtos.SchemaRequest]
  ): F[Entity[domain.Schema]] = {
    repositories.schema.update(
      ???
    )
  }
}
