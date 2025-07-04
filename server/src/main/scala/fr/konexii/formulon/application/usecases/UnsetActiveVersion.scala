package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

import org.typelevel.log4cats.Logger

class UnsetActiveVersion[F[_]: MonadThrow: Logger](
    repositories: Repositories[F]
) {

  def execute(id: UUID, role: Role): F[Unit] =
    for {
      blueprint <- repositories.blueprint.get(id)
      _ <- authorize(blueprint, role)
      _ <- repositories.blueprint.update(
        blueprint.copy(data = blueprint.data.copy(active = None))
      )
    } yield ()

  private def authorize(blueprint: Entity[Blueprint], role: Role): F[Unit] =
    role match {
      case Admin() =>
        Logger[F].info(
          s"Admin disabled active version on blueprint ${blueprint.id}."
        )
      case Editor(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to disable active version on blueprint ${blueprint.id}."
          )
        )
      case Editor(orgName, identifier) =>
        Logger[F].info(
          s"$identifier disabled active version on blueprint ${blueprint.id}."
        )
    }

}
