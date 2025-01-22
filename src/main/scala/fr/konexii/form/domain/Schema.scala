package fr.konexii.form
package domain

import cats.implicits._
import cats.data._
import cats.data.Validated._
import cats.effect.std.UUIDGen
import java.util.UUID
import cats.effect.kernel.Sync

final case class Schema(
    name: String,
    versions: List[Entity[SchemaVersion]] = List(),
    active: Option[Entity[SchemaVersion]] = None
) {

  def unsetActiveVersion(): Schema =
    this.copy(active = None)

  def setActiveVersion(id: UUID): Either[Throwable, Schema] =
    for {
      version <- versions
        .find(entity => entity.id == id)
        .toRight(new Exception(s"Could not find schema version with id $id"))
      schema = this.copy(active = Some(version))
    } yield schema

  def addNewVersion[F[_]: Sync : UUIDGen](
      content: SchemaTree[FieldWithMetadata]
  ): F[Schema] =
    for {
      sv <- SchemaVersion(content)
      entity <- Entity.generateUUID(sv)
    } yield this.copy(versions = entity :: versions)

}

object Schema {

  def apply(name: String): ValidatedNec[Throwable, Schema] =
    (validateName(name.strip)).map(Schema(_))

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
