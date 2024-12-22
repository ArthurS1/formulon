package fr.konexii.form
package application
package repositories

import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

trait SchemaAggregate[F[_]] {
  def get(id: String): F[Entity[Schema]]
  def save(
      schema: Entity[Schema]
  ): F[Entity[Schema]]
  def delete(id: String): F[Unit]
  def update(
      schema: Entity[Schema]
  ): F[Entity[Schema]]
}
