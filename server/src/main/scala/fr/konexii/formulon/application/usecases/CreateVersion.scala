package fr.konexii.formulon.application.usecases

import cats._
import cats.effect._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

import org.typelevel.log4cats.Logger

class CreateVersion[F[_]: Sync: Logger](repositories: Repositories[F]) {

  def execute(
      uuid: UUID,
      tree: Tree[Entity[FieldWithMetadata]],
      role: Role
  ): F[Entity[Version]] =
    for {
      blueprint <- repositories.blueprint.get(uuid)
      result <- blueprint.data.addNewVersion(tree)
      (newBlueprint, newVersion) = result
      _ <- authorize(blueprint, newVersion, role)
      _ <- repositories.blueprint.update(blueprint.map(_ => newBlueprint))
    } yield newVersion

  private def authorize(
      blueprint: Entity[Blueprint],
      version: Entity[Version],
      role: Role
  ): F[Unit] =
    role match {
      case Admin() =>
        Logger[F].info(
          s"Admin created ${version.id} on blueprint ${blueprint.id}."
        )
      case Editor(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to create new version on blueprint ${blueprint.id}."
          )
        )
      case Editor(orgName, identifier) =>
        Logger[F].info(
          s"$identifier created ${version.id} on blueprint ${blueprint.id}."
        )
    }

}
