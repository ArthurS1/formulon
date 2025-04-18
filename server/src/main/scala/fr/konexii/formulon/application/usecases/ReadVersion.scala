package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories

class ReadVersion[F[_]: MonadThrow](respositories: Repositories[F]) {

  def execute(schemaId: UUID, versionId: UUID): F[Entity[Version]] =
    for {
      schema <- respositories.schema.get(schemaId)
      result <- MonadThrow[F].fromOption(
        schema.data.versions.find(e => e.id === versionId),
        new Exception(s"Failed to find schema version with id $versionId.")
      )
    } yield result

}
