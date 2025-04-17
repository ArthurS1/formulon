package fr.konexii.formulon.application.usecases

import cats.effect._
import cats.syntax.all._

import io.circe._
import io.circe.parser.decode

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories
import fr.konexii.formulon.presentation.Serialization._

class CreateVersion[F[_]: Async](
    repositories: Repositories[F],
) {
  def execute(
      uuid: UUID,
      rawVersion: String
  ): F[Entity[Version]] =
    for {
      schema <- repositories.schema.get(uuid)
      //newVersion <- Async[F].fromEither(
      //  decode[Tree[Entity[FieldWithMetadata]]](rawVersion)
      //)
      result <- schema.data.addNewVersion(
        End()
        //TODO: replace newVersion
      ) // I do not intend to add better-monadic-for
      (newSchema, newVersion) = result
      _ <- repositories.schema.update(schema.map(_ => newSchema))
    } yield newVersion

}
