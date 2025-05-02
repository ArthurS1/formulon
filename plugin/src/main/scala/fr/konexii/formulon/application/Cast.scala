package fr.konexii.formulon.application

import fr.konexii.formulon.domain._

import cats.syntax.all._

import shapeless.syntax.typeable._
import shapeless.Typeable

sealed trait CastException extends ValidatorException

final case class CastFailure(typeName: String) extends CastException
final case class NameTypeCheckFailure(typeNameA: String, typeNameB: String)
    extends CastException

object Casts {

  implicit class FieldOps(f: Field) {

    def to[A <: Field: Typeable](nameCheck: String): Either[CastException, A] =
      if (nameCheck === f.name)
        Either.fromOption(
          f.cast[A],
          CastFailure(nameCheck)
        )
      else
        Left(NameTypeCheckFailure(nameCheck, f.name))

  }

  implicit class AnswerOps(a: Answer) {

    def to[A <: Answer: Typeable](nameCheck: String): Either[CastException, A] =
      if (nameCheck === a.name)
        Either.fromOption(
          a.cast[A],
          CastFailure(nameCheck)
        )
      else
        Left(NameTypeCheckFailure(nameCheck, a.name))

  }

}
