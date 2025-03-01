package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.application.Repositories

class GetSubmissionsForVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(schemaId: UUID, versionId: UUID): F[List[Entity[Submission]]] =
    for {
      schema <- repositories.schema.get(schemaId)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(e => e.id === versionId),
        new Exception(s"Failed to find the schema with id $schemaId.")
      )
      result <- repositories.submission.getAll(version)
    } yield result

}
