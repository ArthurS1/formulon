package fr.konexii.form.application.usecases

import cats._
import cats.data._
import cats.syntax.all._
import cats.effect.std.UUIDGen

import fr.konexii.form.domain._
import fr.konexii.form.application.Repositories
import fr.konexii.form.application.utils.CompositeException
import fr.konexii.form.application.dtos.CreateSchemaRequest

class CreateSchema[F[_]: MonadThrow: UUIDGen](repositories: Repositories[F]) {

  def execute(newSchemaRequest: CreateSchemaRequest): F[Entity[Schema]] =
    for {
      uuid <- UUIDGen[F].randomUUID
      newSchema <- MonadThrow[F].fromValidated(
        Schema(name = newSchemaRequest.name).leftMap(CompositeException)
      )
      newSchemaEntity <- repositories.schema.create(Entity(uuid, newSchema))
    } yield newSchemaEntity

}
