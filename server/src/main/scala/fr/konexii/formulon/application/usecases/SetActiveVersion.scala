package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

import org.typelevel.log4cats.Logger

class SetActiveVersion[F[_]: MonadThrow: Logger](
    repositories: Repositories[F]
) {

  def execute(schemaId: UUID, versionId: UUID, role: Role): F[Unit] =
    for {
      schema <- repositories.blueprint.get(schemaId)
      version <- MonadThrow[F].fromOption(
        schema.data.versions.find(_.id === versionId),
        new Exception(s"Could not find version $versionId.")
      )
      _ <- authorize(schema, version, role)
      _ <- repositories.blueprint.update(
        schema.copy(data = schema.data.copy(active = Some(version)))
      )
    } yield ()

  private def authorize(
      blueprint: Entity[Blueprint],
      version: Entity[Version],
      role: Role
  ): F[Unit] =
    role match {
      case Admin() =>
        Logger[F].info(
          s"Admin set ${version.id} active on blueprint ${blueprint.id}."
        )
      case Org(orgName, identifier) if (orgName =!= blueprint.data.tag) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to set active version on blueprint ${blueprint.id}."
          )
        )
      case Org(orgName, identifier) =>
        Logger[F].info(
          s"$identifier set ${version.id} active on blueprint ${blueprint.id}."
        )
    }

}
