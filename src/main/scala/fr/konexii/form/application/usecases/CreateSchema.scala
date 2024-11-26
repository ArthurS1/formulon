package fr.konexii.form
package usecases

import cats._
import cats.implicits._
import cats.syntax._
import cats.instances._
import cats.effect.std.UUIDGen

import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.Schema
import fr.konexii.form.domain.Entity

class CreateSchema[F[_]](repositories: Repositories[F])(implicit F: Monad[F], G: UUIDGen[F]) {
  def execute(newSchema: Schema): F[Entity[Schema]] =
    for {
      uuid <- G.randomUUID
      newSchemaEntity <- repositories.schema.save(Entity(uuid, newSchema))
    } yield newSchemaEntity
}
