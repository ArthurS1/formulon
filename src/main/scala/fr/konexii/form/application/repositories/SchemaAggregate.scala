package fr.konexii.form.application.repositories

import fr.konexii.form.domain._

import java.util.UUID

trait SchemaAggregate[F[_]] {

  def get(id: UUID): F[Entity[Schema]]

  def create(schema: Entity[Schema]): F[Entity[Schema]]

  def delete(schema: Entity[Schema]): F[Unit]

  def update(schema: Entity[Schema]): F[Entity[Schema]]

}
