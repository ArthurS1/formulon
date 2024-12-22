package fr.konexii.form
package application
package usecases

import cats._
import cats.implicits._
import cats.syntax._
import cats.instances._
import cats.effect.std.UUIDGen

import fr.konexii.form.application.Repositories
import fr.konexii.form.application.dtos.SchemaRequest
import fr.konexii.form.application.dtos.SchemaRequest.implicits._
import fr.konexii.form.domain.Entity
import fr.konexii.form.domain.Schema

class CreateSchema[F[_]](repositories: Repositories[F])(implicit F: Monad[F], G: UUIDGen[F]) {
  def execute(newSchema: SchemaRequest): F[Entity[Schema]] =
    for {
      uuid <- G.randomUUID
      newSchemaEntity <- repositories.schema.save(Entity(uuid, newSchema))
    } yield newSchemaEntity
}
