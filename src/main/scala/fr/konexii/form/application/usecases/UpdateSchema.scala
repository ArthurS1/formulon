package fr.konexii.form
package application
package usecases

import fr.konexii.form._

class UpdateSchema[F[_]](repositories: Repositories[F]) {
  def execute(update : domain.Entity[domain.Schema]): F[domain.Entity[domain.Schema]] = {
    repositories.schema.update(update)
  }
}
