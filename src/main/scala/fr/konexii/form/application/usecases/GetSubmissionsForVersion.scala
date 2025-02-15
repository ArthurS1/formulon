package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class GetSubmissionsForVersion[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(schemaId: String, versionId: String): F[List[Entity[Submission]]] =
    for {
      schemaUuid <- schemaId.toUuid
      versionUuid <- versionId.toUuid
      schema <- repositories.schema.get(schemaUuid)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(e => e.id === versionUuid),
        new Exception(s"Failed to find the schema with id $schemaUuid.")
      )
      result <- repositories.submission.getAll(version)
    } yield result

}
