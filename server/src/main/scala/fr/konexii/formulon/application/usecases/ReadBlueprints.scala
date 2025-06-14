package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

class ReadBlueprints[F[_]: Functor](repositories: Repositories[F]) {

  def execute(role: Role): F[List[Entity[Blueprint]]] =
    role match {
      case Admin() => repositories.blueprint.getAll()
      case Org(orgName, identifier) =>
        repositories.blueprint
          .getAll()
          .map(
            _.filter(_.data.tag === orgName)
          )
    }

}
