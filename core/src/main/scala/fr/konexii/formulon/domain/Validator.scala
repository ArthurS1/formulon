package fr.konexii.formulon.domain

import cats.data._
import cats.syntax.all._

import fr.konexii.formulon.domain.Tree._
import java.util.UUID

trait ValidatorException
final case class RequiredFieldNotFound(id: UUID) extends ValidatorException
final case class TypesDiffer(id: UUID, typeA: String, typeB: String)
    extends ValidatorException

object Validator {

  type Association = (Option[Answer], FieldWithMetadata)

  type Validation =
    Zipper[Association] => Either[
      NonEmptyChain[ValidatorException],
      Zipper[Association]
    ]

  def validate(
      t: Tree[Entity[FieldWithMetadata]],
      s: Submission,
      f: Validation
  ): Either[NonEmptyChain[ValidatorException], Submission] = {
    validateSingle(
      Zipper(association(t, s)),
      f
    ).map(_ => s)
  }

  def validateAll(
      current: Zipper[Association],
      f: Validation
  ): Either[NonEmptyChain[ValidatorException], Zipper[Association]] =
    validateSingle(current, f).flatMap(z =>
      z.focus match {
        case End() => Right(z)
        case _     => validateSingle(z, f)
      }
    )

  def validateSingle(
      z: Zipper[Association],
      f: Validation
  ): Either[NonEmptyChain[ValidatorException], Zipper[Association]] =
    z.content match {
      case Some(Entity(id, association)) if failsRequirementCheck(association) =>
        Left(NonEmptyChain.one(RequiredFieldNotFound(id)))
      case Some(Entity(id, (Some(ans), fwm))) if ans.name =!= fwm.field.name =>
        Left(NonEmptyChain.one(TypesDiffer(id, ans.name, fwm.field.name)))
      case _        => f(z)
    }

  def failsRequirementCheck(
      a: Association
  ): Boolean = a match {
    case (None, fwm) if fwm.required => true
    case _                           => false
  }

  def association(
      t: Tree[Entity[FieldWithMetadata]],
      s: Submission
  ): Tree[Entity[Association]] =
    t.map(a => a.map(b => (s.answers.find(c => a.id === c.id).map(_.data), b)))
}
