package fr.konexii.formulon.application.repositories

import fr.konexii.formulon.domain._

import java.util.UUID

trait BlueprintAggregate[F[_]] {

  def get(id: UUID): F[Entity[Blueprint]]

  def create(blueprint: Entity[Blueprint]): F[Entity[Blueprint]]

  def delete(blueprint: Entity[Blueprint]): F[Unit]

  def update(blueprint: Entity[Blueprint]): F[Entity[Blueprint]]

}
