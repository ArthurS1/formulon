package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std.UUIDGen

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories
import fr.konexii.formulon.application.utils.CompositeException
import fr.konexii.formulon.application.dtos.CreateSchemaRequest

class CreateSchema[F[_]: MonadThrow: UUIDGen](repositories: Repositories[F]) {

  def execute(newSchemaRequest: CreateSchemaRequest): F[Entity[Blueprint]] =
    for {
      uuid <- UUIDGen[F].randomUUID
      newSchema <- MonadThrow[F].fromValidated(
        Blueprint(name = newSchemaRequest.name).leftMap(CompositeException)
      )
      newSchemaEntity <- repositories.schema.create(Entity(uuid, newSchema))
    } yield newSchemaEntity

}
