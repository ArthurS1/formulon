package fr.konexii.form
package application
package usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain._

class ReadVersionList[F[_]: MonadThrow](repositories: Repositories[F]) {

  def execute(
      id: String
  ): F[List[Entity[SchemaVersion]]] =
    for {
      uuid <- MonadThrow[F].catchNonFatal(UUID.fromString(id))
      schema <- repositories.schema.get(uuid)
    } yield schema.data.versions

}
