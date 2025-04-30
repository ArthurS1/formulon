package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

import org.typelevel.log4cats.Logger

class ReadVersionList[F[_]: MonadThrow: Logger](repositories: Repositories[F]) {

  def execute(id: UUID, role: Role): F[List[Entity[Version]]] =
    for {
      blueprint <- repositories.blueprint.get(id)
      _ <- authorize(blueprint, role)
    } yield blueprint.data.versions

  private def authorize(
      blueprint: Entity[Blueprint],
      role: Role
  ): F[Unit] =
    role match {
      case Admin() =>
        Logger[F].info(
          s"Admin read all versions on blueprint ${blueprint.id}."
        )
      case Org(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to read all versions on blueprint ${blueprint.id}."
          )
        )
      case Org(orgName, identifier) =>
        Logger[F].info(
          s"$identifier read all versions on blueprint ${blueprint.id}."
        )
    }

}
