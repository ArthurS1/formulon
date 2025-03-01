package fr.konexii.form.domain

import cats._
import cats.data._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.form.domain.fields._
import fr.konexii.form.domain.answer._
import fr.konexii.form.domain.SchemaTree._

sealed trait ValidatorError

final case class RequiredNotFound(id: UUID) extends ValidatorError
final case class TypeMissmatch(id: UUID, type1: String, type2: String)
    extends ValidatorError
final case class MultiplePathsDetected(id1: UUID, id2: UUID)
    extends ValidatorError
final case class PathStopedBeforeEndAtBranch(id1: UUID, id2: UUID)
    extends ValidatorError
final case class PathStopedBeforeEndAtTrunk(id: UUID) extends ValidatorError
// On some cases where we should never reach (ex. missing expected id on the next node)
final case class UnexpectedError() extends ValidatorError

object ValidatorError extends ValidatorErrorInstances

trait ValidatorErrorInstances {

  implicit def showForValidatorError: Show[ValidatorError] =
    new Show[ValidatorError] {
      def show(t: ValidatorError): String = "[analysis] ".concat(t match {
        case RequiredNotFound(id) =>
          s"Could not find field corresponding to $id [required]"
        case TypeMissmatch(id, type1, type2) =>
          s"Type missmatch for field $id [$type1 and $type2 differ]"
        case MultiplePathsDetected(id1, id2) =>
          s"Multiple possible path detected in submission [$id1 and $id2 have been filled]"
        case PathStopedBeforeEndAtBranch(id1, id2) =>
          s"No possible path detected in submission [$id1 and $id2 are empty]"
        case PathStopedBeforeEndAtTrunk(id) =>
          s"No possible path in submission at $id"
        case UnexpectedError() =>
          s"Unexpected error was produced [this error should never happen]"
      })
    }

}

object Validator {

  def validate(
      st: SchemaTree[Entity[FieldWithMetadata]],
      answers: List[Entity[Answer]]
  ): ValidatedNec[ValidatorError, List[Entity[Answer]]] = analyze(
    association(st, answers)
  )

  type Association = (Option[Entity[Answer]], Entity[FieldWithMetadata])

  def association(
      st: SchemaTree[Entity[FieldWithMetadata]],
      answers: List[Entity[Answer]]
  ): SchemaTree[Association] =
    st.map(f => (answers.find(a => f.id === a.id), f))

  // Verifies that all required nodes are present and that all only a single
  // path is used until the end.
  def analyze(
      at: SchemaTree[Association]
  ): ValidatedNec[ValidatorError, List[Entity[Answer]]] =
    (requiredFieldsAnalysis(at), singlePathAnalysis(at).toValidatedNec).mapN {
      case (_, _) =>
        at.foldLeft[List[Entity[Answer]]](List()) {
          case (acc, (Some(answer), _)) => answer :: acc
          case (acc, _)                     => acc
        }
    }

  def singlePathAnalysis(
      at: SchemaTree[Association]
  ): Either[ValidatorError, SchemaTree[Association]] =
    at match {
      case End() => Right(End())
      case Trunk(content, next) if isFilled(next) =>
        for {
          nat <- singlePathAnalysis(next)
        } yield Trunk(content, nat)
      case Trunk(content, next) if !isFilled(next) =>
        next
          .map(_._2)
          .id
          .map(PathStopedBeforeEndAtTrunk)
          .getOrElse(UnexpectedError())
          .asLeft
      case Branch(content, next, out) if isFilled(next) ^ isFilled(out) =>
        for {
          nat <- singlePathAnalysis(next)
          oat <- singlePathAnalysis(out)
        } yield Branch(content, nat, oat)
      case Branch(content, next, out) if isFilled(next) && isFilled(out) =>
        (next.map(_._2).id, out.map(_._2).id)
          .mapN((a: UUID, b: UUID) => MultiplePathsDetected(a, b))
          .getOrElse(UnexpectedError())
          .asLeft
      case Branch(content, next, out) if !isFilled(next) && !isFilled(out) =>
        (next.map(_._2).id, out.map(_._2).id)
          .mapN((a: UUID, b: UUID) => PathStopedBeforeEndAtBranch(a, b))
          .getOrElse(UnexpectedError())
          .asLeft
      case _ => Left(UnexpectedError())
    }

  def isFilled(
      at: SchemaTree[Association]
  ): Boolean = at match {
    case End()                           => true
    case Trunk((Some(_), _), next)       => true
    case Branch((Some(_), _), next, out) => true
    case _                               => false
  }

  def requiredFieldsAnalysis(
      at: SchemaTree[Association]
  ): ValidatedNec[ValidatorError, SchemaTree[Association]] =
    at.map(assoc =>
      assoc match {
        case (None, Entity(id, FieldWithMetadata(_, required, _)))
            if required === true =>
          RequiredNotFound(id).invalidNec
        case _ => assoc.validNec
      }
    ).sequence

  def fieldValidation(
      answerEntity: Entity[Answer],
      field: Entity[FieldWithMetadata]
  ): ValidatedNec[ValidatorError, Answer] =
    (answerEntity, field) match {
      case (
            Entity(_, answer: answer.Text),
            Entity(_, FieldWithMetadata(_, _, _: fields.Text))
          ) =>
        answer.validNec
      case (Entity(id, _), _) =>
        /*
         * TODO : We really want to make the types available through some kind of table maybe.
         * This should be considered when we end up doing plugins to add fields.
         */

        TypeMissmatch(id, "???", "???").invalidNec
    }

  /*
   * Sample validation for the text answer. It does not validate anything.
   */

  def validateText(
      rsp: answer.Text
  ): ValidatedNec[ValidatorError, Answer] = rsp.validNec

}
