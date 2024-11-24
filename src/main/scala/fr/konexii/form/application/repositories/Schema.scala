package fr.konexii.form
package application
package repositories

import fr.konexii.form._

trait Schema[F[_]] {
  def get(id: String): F[Option[domain.Schema]]
  def save(schema: domain.Schema): F[domain.Schema]
}
