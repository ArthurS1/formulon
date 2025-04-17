package fr.konexii.formulon.application.repositories

import fr.konexii.formulon.domain._

import java.util.UUID

trait SchemaAggregate[F[_]] {

  def get(id: UUID): F[Entity[Blueprint]]

  def create(schema: Entity[Blueprint]): F[Entity[Blueprint]]

  def delete(schema: Entity[Blueprint]): F[Unit]

  def update(schema: Entity[Blueprint]): F[Entity[Blueprint]]

}
