package fr.konexii.form
package application
package usecases

import cats._
import cats.effect._
import cats.syntax.all._

import io.circe.parser.decode

import java.util.UUID

import fr.konexii.form.presentation.Serialization._
import fr.konexii.form.domain._
import fr.konexii.form.application.dtos.CreateVersionRequest

class CreateVersion[F[_]: Async](repositories: Repositories[F]) {

  def execute(
      id: String,
      rawNewVersion: String
  ): F[Entity[SchemaVersion]] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      schemasEntity <- repositories.schema.get(uuid)
      newVersion <- Async[F].fromEither(decode[SchemaTree[FieldWithMetadata]](rawNewVersion)(decoderForSchemaTree))
      // I do not intend to add better-monadic-for
      result <- schemasEntity.data.addNewVersion(newVersion)
      newSchemaEntity = schemasEntity.map(_ => result._1)
      _ <- repositories.schema.update(newSchemaEntity)
    } yield result._2

}
