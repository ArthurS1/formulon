package fr.konexii.form.application.usecases

import cats._
import cats.effect._
import cats.syntax.all._

import io.circe.parser.decode

import fr.konexii.form.domain._
import fr.konexii.form.domain.fields._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories
import fr.konexii.form.presentation.Serialization._

class CreateVersion[F[_]: Async](repositories: Repositories[F]) {

  def execute(
      id: String,
      rawVersion: String
  ): F[Entity[SchemaVersion]] =
    for {
      uuid <- id.toUuid
      schema <- repositories.schema.get(uuid)
      newVersion <- Async[F].fromEither(
        decode[SchemaTree[Entity[FieldWithMetadata]]](rawVersion)
      )
      result <- schema.data.addNewVersion(
        newVersion
      ) // I do not intend to add better-monadic-for
      newSchema = schema.map(_ => result._1)
      _ <- repositories.schema.update(newSchema)
    } yield result._2

}
