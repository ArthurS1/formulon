package fr.konexii.form
package application
package usecases

import java.util.UUID

import fr.konexii.form.application.Repositories

class DeleteSchema[F[_]](repositories: Repositories[F]) {

  def execute(id: String): F[Unit] = {
    repositories.schema.delete(id)
  }

}
