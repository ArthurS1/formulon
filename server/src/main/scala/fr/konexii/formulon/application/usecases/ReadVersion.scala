package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

import org.typelevel.log4cats.Logger

class ReadVersion[F[_]: MonadThrow: Logger](respositories: Repositories[F]) {

  def execute(blueprintId: UUID, versionId: UUID, role: Role): F[Entity[Version]] =
    for {
      blueprint <- respositories.blueprint.get(blueprintId)
      result <- MonadThrow[F].fromOption(
        blueprint.data.versions.find(e => e.id === versionId),
        new Exception(s"Failed to find schema version with id $versionId.")
      )
      _ <- authorize(blueprint, result, role)
    } yield result

  private def authorize(
      blueprint: Entity[Blueprint],
      version: Entity[Version],
      role: Role
  ): F[Unit] =
    role match {
      case Admin() =>
        Logger[F].info(
          s"Admin read ${version.id} on blueprint ${blueprint.id}."
        )
      case Editor(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to set active version on blueprint ${blueprint.id}."
          )
        )
      case Editor(orgName, identifier) =>
        Logger[F].info(
          s"$identifier read ${version.id} on blueprint ${blueprint.id}."
        )
    }

}
