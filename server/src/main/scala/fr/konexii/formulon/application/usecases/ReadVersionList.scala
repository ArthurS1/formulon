package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Repositories

class ReadVersionList[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      id: UUID
  ): F[List[Entity[Version]]] =
    for {
      schema <- repositories.schema.get(id)
    } yield schema.data.versions

}
