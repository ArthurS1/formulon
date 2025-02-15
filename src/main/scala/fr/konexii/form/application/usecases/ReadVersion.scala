package fr.konexii.form.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.form.domain._
import fr.konexii.form.application.utils.uuid._
import fr.konexii.form.application.Repositories

class ReadVersion[F[_]: MonadThrow](respositories: Repositories[F]) {

  def execute(schemaId: String, versionId: String): F[Entity[SchemaVersion]] =
    for {
      schemaUuid <- schemaId.toUuid
      versionUuid <- versionId.toUuid
      schema <- respositories.schema.get(schemaUuid)
      result <- MonadThrow[F].fromOption(
        schema.data.versions.find(e => e.id === versionUuid),
        new Exception(s"Failed to find schema version with id $versionId.")
      )
    } yield result

}
