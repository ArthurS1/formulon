package fr.konexii.formulon.domain

import cats.data._
import cats.syntax.all._

import fr.konexii.formulon.domain.Tree._
import java.util.UUID

sealed trait ValidatorException[E] extends KeyedException
sealed case class RequiredFieldNotFound[E](id: UUID)
   extends ValidatorException[E]
sealed case class TypesDiffer[E](id: UUID, typeA: String, typeB: String)
    extends ValidatorException[E]
sealed case class PluginException[E](e: E) extends ValidatorException[E]

object Validator {

  type Association = (Option[Answer], FieldWithMetadata)

  type Validation[E] =
    Zipper[Association] => Either[
      NonEmptyChain[E],
      Zipper[Association]
    ]

  def validate[E](
      t: Tree[Entity[FieldWithMetadata]],
      s: Submission,
      f: Validation[E]
  ): Either[NonEmptyChain[ValidatorException[E]], Submission] = {
    validateSingle(
      Zipper(association(t, s)),
      f
    ).map(_ => s)
  }

  def validateAll[E](
      current: Zipper[Association],
      f: Validation[E]
  ): Either[NonEmptyChain[ValidatorException[E]], Zipper[Association]] =
    validateSingle(current, f).flatMap(z =>
      z.focus match {
        case End() => Right(z)
        case _     => validateSingle(z, f)
      }
    )

  def validateSingle[E](
      z: Zipper[Association],
      f: Validation[E]
  ): Either[NonEmptyChain[ValidatorException[E]], Zipper[Association]] =
    z.content match {
      case Some(Entity(id, association))
          if failsRequirementCheck(association) =>
        Left(NonEmptyChain.one(RequiredFieldNotFound(id)))
      case Some(Entity(id, (Some(ans), fwm))) if ans.name =!= fwm.field.name =>
        Left(NonEmptyChain.one(TypesDiffer(id, ans.name, fwm.field.name)))
      case _ => f(z).left.map(_.map(PluginException(_)))
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
