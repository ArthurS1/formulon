package fr.konexii.formulon
package application

import fr.konexii.formulon.application._

trait Repositories[F[_]] {
  def blueprint: repositories.BlueprintAggregate[F]
  def submission: repositories.SubmissionAggregate[F]
}
