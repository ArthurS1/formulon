package fr.konexii.form
package application

import fr.konexii.form.application._

trait Repositories[F[_]] {
  def schema: repositories.Schema[F]
}
