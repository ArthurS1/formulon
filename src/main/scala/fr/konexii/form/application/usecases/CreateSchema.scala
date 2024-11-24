package fr.konexii.form
package usecases

import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.Schema

class CreateSchema[F[_]](repositories: Repositories[F]) {
  def execute(newSchema: Schema): F[Schema] = repositories.schema.save(newSchema)
}
