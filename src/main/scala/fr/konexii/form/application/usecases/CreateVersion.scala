package fr.konexii.form.application.usecases

import cats.effect._
import cats.syntax.all._

import io.circe.parser.decode

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.domain.field._
import fr.konexii.form.application.Repositories
import fr.konexii.form.presentation.Serialization._

class CreateVersion[F[_]: Async](repositories: Repositories[F]) {

  def execute(
      uuid: UUID,
      rawVersion: String
  ): F[Entity[SchemaVersion]] =
    for {
      schema <- repositories.schema.get(uuid)
      newVersion <- Async[F].fromEither(
        decode[SchemaTree[Entity[FieldWithMetadata]]](rawVersion)
      )
      result <- schema.data.addNewVersion(
        newVersion
      ) // I do not intend to add better-monadic-for
      (newSchema, newVersion) = result
      _ <- repositories.schema.update(schema.map(_ => newSchema))
    } yield newVersion

}
