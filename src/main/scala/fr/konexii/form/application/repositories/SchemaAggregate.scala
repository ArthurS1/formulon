package fr.konexii.form
package application
package repositories

import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

import java.util.UUID

trait SchemaAggregate[F[_]] {
  def get(id: UUID): F[Entity[Schema]]
  def create(
      schema: Entity[Schema]
  ): F[Entity[Schema]]
  def delete(id: UUID): F[Unit]
  def update(
      schema: Entity[Schema]
  ): F[Entity[Schema]]
}
