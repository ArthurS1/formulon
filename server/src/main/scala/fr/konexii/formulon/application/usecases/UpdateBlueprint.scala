package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.dtos.UpdateBlueprintRequest

import org.typelevel.log4cats.Logger

class UpdateBlueprint[F[_]: MonadThrow: Logger](repositories: Repositories[F]) {

  def execute(
      update: UpdateBlueprintRequest,
      id: UUID,
      role: Role
  ): F[Entity[Blueprint]] =
    for {
      blueprint <- repositories.blueprint.get(id)
      _ <- authorize(blueprint, role)
      updatedBlueprint <- repositories.blueprint.update(
        blueprint.copy(data = blueprint.data.copy(name = update.name))
      )
    } yield updatedBlueprint

  private def authorize(blueprint: Entity[Blueprint], role: Role): F[Unit] =
    role match {
      case Admin() => Logger[F].info(s"Admin updated blueprint ${blueprint.id}.")
      case Editor(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to update blueprint ${blueprint.id}."
          )
        )
      case Editor(orgName, identifier) =>
        Logger[F].info(s"$identifier updated blueprint ${blueprint.id}.")
    }

}
