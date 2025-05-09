package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories

class GetSubmissionsForVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(blueprintId: UUID, versionId: UUID): F[List[Entity[Submission]]] =
    for {
      schema <- repositories.blueprint.get(blueprintId)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(e => e.id === versionId),
        new Exception(s"Failed to find the schema with id $blueprintId.")
      )
      result <- repositories.submission.getAll(version)
    } yield result

}
