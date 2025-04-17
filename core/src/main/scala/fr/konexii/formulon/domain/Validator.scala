package fr.konexii.formulon.domain

import cats.data._
import cats.syntax.all._

import fr.konexii.formulon.domain.Tree._

object Validator {

  type Association = (Option[Entity[Answer]], Entity[FieldWithMetadata])

  type Validation =
    Tree[Association] => Either[NonEmptyChain[Throwable], Tree[Association]]

  def validate(
      t: Tree[Entity[FieldWithMetadata]],
      s: Submission,
      f: Validation
  ): Either[NonEmptyChain[Throwable], Submission] = analyze(
    association(t, s),
    f
  ).map(_ => s)

  private def analyze(
      t: Tree[Association],
      f: Validation
  ): Either[NonEmptyChain[Throwable], Tree[Association]] = f(t).flatMap(t =>
    t match {
      case Trunk((None, Entity(id, FieldWithMetadata(_, true, _))), _) =>
        Left(
          NonEmptyChain.one(
            new Exception(s"Required field at id $id not found")
          )
        )
      case t @ Trunk(_, _)     => analyze(t, f)
      case t @ Branch(_, _, _) => analyze(t, f)
      case t @ End()           => Right(t)
    }
  )

  private def association(
      t: Tree[Entity[FieldWithMetadata]],
      s: Submission
  ): Tree[Association] =
    t.map(f => (s.answers.find(a => f.id === a.id), f))

}
