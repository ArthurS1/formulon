package fr.konexii.formulon.application.usecases

import cats._
import cats.effect._
import cats.syntax.all._

import io.circe._
import io.circe.parser.decode

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.utils.UnauthorizedException
import fr.konexii.formulon.presentation.Serialization._

import org.typelevel.log4cats.Logger

class CreateVersion[F[_]: Sync: Logger](repositories: Repositories[F], plugins: List[Plugin]) {

  implicit val decoder: Decoder[FieldWithMetadata] =
    decoderForFieldWithMetadata(plugins)

  def execute(
      uuid: UUID,
      rawVersion: String,
      role: Role
  ): F[Entity[Version]] =
    for {
      blueprint <- repositories.blueprint.get(uuid)
      tree <- MonadThrow[F].fromEither(
        decode[Tree[Entity[FieldWithMetadata]]](rawVersion)
      )
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
      case Org(orgName, identifier) if (orgName =!= blueprint.data.orgName) =>
        MonadThrow[F].raiseError[Unit](
          new UnauthorizedException(
            s"$identifier unauthorized to create new version on blueprint ${blueprint.id}."
          )
        )
      case Org(orgName, identifier) =>
        Logger[F].info(
          s"$identifier created ${version.id} on blueprint ${blueprint.id}."
        )
    }

}
