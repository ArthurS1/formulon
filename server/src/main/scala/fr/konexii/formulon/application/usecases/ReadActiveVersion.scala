package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories

class ReadActiveVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(id: UUID): F[Entity[Version]] =
    for {
      schema <- repositories.schema.get(id)
      activeVersion <- MonadThrow[F].fromOption(
        schema.data.active,
        new Exception(s"No active version for schema with id $id.")
      )
    } yield activeVersion

}
