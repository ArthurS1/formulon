package fr.konexii.formulon.domain

import cats.effect._
import cats.data._
import cats.syntax.all._
import cats.data.Validated._
import cats.effect.std.UUIDGen

import fr.konexii.formulon.domain.Invariants._

import java.util.UUID

sealed trait BlueprintException
final case class CouldNotFindSchemaVersion(id: UUID) extends BlueprintException

final case class Blueprint(
    name: String,
    tag: String,
    versions: List[Entity[Version]] = List(),
    active: Option[Entity[Version]] = None
) {

  def unsetActiveVersion(): Blueprint =
    this.copy(active = None)

  def setActiveVersion(id: UUID): Either[BlueprintException, Blueprint] =
    for {
      version <- Either.fromOption(
        versions.find(entity => entity.id == id),
        CouldNotFindSchemaVersion(id)
      )
      schema = this.copy(active = Some(version))
    } yield schema

  def addNewVersion[F[_]: Sync: UUIDGen](
      content: Tree[Entity[FieldWithMetadata]]
  ): F[(Blueprint, Entity[Version])] =
    for {
      sv <- Version(content)
      entity <- Entity.generateUUID(sv)
    } yield (this.copy(versions = entity :: versions), entity)

}

object Blueprint {

  def apply(
      name: String,
      tag: String
  ): ValidatedNec[InvariantsException, Blueprint] =
    (validateName(name.strip), validateTag(tag.strip)).mapN(new Blueprint(_, _))

  def validateName(name: String): ValidatedNec[InvariantsException, String] =
    isNotBlank(name) *> isNotMoreThan(80, name) *> isOnlyUnicodeLetters(name)

  def validateTag(tag: String): ValidatedNec[InvariantsException, String] =
    isNotBlank(tag) *> isNotMoreThan(40, tag) *> isOnlyAlphasAndDigits(tag)

}
