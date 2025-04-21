package fr.konexii.formulon.domain

import cats.effect._
import cats.data._
import cats.syntax.all._
import cats.data.Validated._
import cats.effect.std.UUIDGen

import java.util.UUID

final case class Blueprint(
    name: String,
    orgName: String,
    versions: List[Entity[Version]] = List(),
    active: Option[Entity[Version]] = None
) {

  def unsetActiveVersion(): Blueprint =
    this.copy(active = None)

  def setActiveVersion(id: UUID): Either[Throwable, Blueprint] =
    for {
      version <- Either.fromOption(
        versions.find(entity => entity.id == id),
        new Exception(s"Could not find schema version with id $id.")
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

  def apply(name: String, orgName: String): ValidatedNec[Throwable, Blueprint] =
    (validateName(name.strip)).map(Blueprint(_, orgName))

  def validateName(name: String): ValidatedNec[Throwable, String] =
    isNotBlank(name) *> isNotMoreThan(80, name) *> isOnlyAlphasAndDigits(name)

  def isNotBlank(s: String): ValidatedNec[Throwable, String] =
    if (s.isEmpty()) (new Exception("is empty")).invalidNec else s.validNec

  def isNotMoreThan(nbChar: Int, s: String) =
    if (s.size > nbChar)
      (new Exception(s"is too wide (max $nbChar characters)")).invalidNec
    else s.validNec

  def isOnlyAlphasAndDigits(s: String) =
    if (s.matches("([A-Za-z0-9 ])*"))
      s.validNec
    else
      (new Exception("should contain only letters and digits")).invalidNec

}
