package fr.konexii.formulon.application.usecases

import cats._
import cats.syntax.all._
import cats.effect.std.UUIDGen

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.dtos.CreateSchemaRequest

import org.typelevel.log4cats.Logger

class CreateBlueprint[F[_]: MonadThrow: UUIDGen: Logger](
    repositories: Repositories[F]
) {

  implicit val showForValidationException: Show[ValidationException] = ???

  type OrgName = String

  def execute(
      newSchemaRequest: CreateSchemaRequest,
      role: Role
  ): F[Entity[Blueprint]] =
    for {
      tag <- authorize(role)
      uuid <- UUIDGen[F].randomUUID
      newBlueprint <- MonadThrow[F].fromValidated(
        Blueprint(newSchemaRequest.name, tag)
          .leftMap(err =>
            CompositeException(err.map(Show[ValidationException].show(_)))
          )
      )
      newBlueprintEntity <- repositories.blueprint.create(
        Entity(uuid, newBlueprint)
      )
    } yield newBlueprintEntity

  private def authorize(role: Role): F[OrgName] =
    role match {
      case Admin() =>
        MonadThrow[F].raiseError[OrgName](
          new UnauthorizedException(
            s"Admin unauthorized to create blueprint."
          )
        )
      case Org(orgName, identifier) =>
        Logger[F]
          .info(s"$identifier creating blueprint.")
          .map(_ => orgName)
    }

}
