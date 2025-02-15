package fr.konexii.form.application.repositories

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._

import java.util.UUID

trait SubmissionAggregate[F[_]] {

  def create(
      submission: Entity[Submission],
      version: Entity[SchemaVersion]
  ): F[Entity[Submission]]

  def getAll(version: Entity[SchemaVersion]): F[List[Entity[Submission]]]

}
