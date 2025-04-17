package fr.konexii.formulon.application.repositories

import fr.konexii.formulon.domain._

trait SubmissionAggregate[F[_]] {

  def create(
      submission: Entity[Submission],
      version: Entity[Version]
  ): F[Entity[Submission]]

  def getAll(version: Entity[Version]): F[List[Entity[Submission]]]

}
