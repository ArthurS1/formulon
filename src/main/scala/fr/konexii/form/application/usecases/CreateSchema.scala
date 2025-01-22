package fr.konexii.form
package application
package usecases

import cats._
import cats.implicits._
import cats.syntax._
import cats.instances._
import cats.effect.std.UUIDGen

import fr.konexii.form.application.Repositories
import fr.konexii.form.application.dtos.CreateSchemaRequest
import fr.konexii.form.application.dtos.CreateSchemaRequest.implicits._
import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

class CreateSchema[F[_]](repositories: Repositories[F])(implicit F: Monad[F], G: UUIDGen[F]) {
  def execute(newSchema: CreateSchemaRequest): F[Entity[Schema]] =
    for {
      uuid <- G.randomUUID
      newSchemaEntity <- repositories.schema.save(Entity(uuid, newSchema))
    } yield newSchemaEntity
}
