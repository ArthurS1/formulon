package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.utils.UnauthorizedException

import org.typelevel.log4cats.Logger

class ReadBlueprint[F[_]: MonadThrow : Logger](repositories: Repositories[F]) {

  def execute(id: UUID, role: Role): F[Entity[Blueprint]] =
    for {
      result <- repositories.blueprint.get(id)
      _ <- authorize(result, role)
    } yield result

  private def authorize(blueprint: Entity[Blueprint], role: Role): F[Unit] =
    role match {
      case Admin() => Logger[F].info(s"Admin updated blueprint ${blueprint.id}.")
      case Org(orgName, identifier) if (orgName =!= blueprint.data.orgName) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to update blueprint ${blueprint.id}."
          )
        )
      case Org(orgName, identifier) =>
        Logger[F].info(s"$identifier updated blueprint ${blueprint.id}.")
    }

}
