package fr.konexii.formulon.application

import fr.konexii.formulon.domain._

import cats.syntax.all._

import shapeless.syntax.typeable._
import shapeless.Typeable

object Casts {

  implicit class FieldOps(f: Field) {

    def to[A <: Field: Typeable](nameCheck: String): Either[Throwable, A] =
      (
        if (nameCheck === f.name)
          Either.fromOption(
            f.cast[A],
            s"Failure in casting a $nameCheck at library level."
          )
        else
          Left(s"Cannot cast a $nameCheck into a ${f.name}.")
      ).left.map(msg => new Exception(s"Field error: $msg"))

  }

  implicit class AnswerOps(a: Answer) {

    def to[A <: Answer: Typeable](nameCheck: String): Either[Throwable, A] =
      (
        if (nameCheck === a.name)
         Either.fromOption(
           a.cast[A],
           s"Failure in casting a $nameCheck at library level."
         )
       else
         Left(s"Cannot cast a $nameCheck into a ${a.name}.")
      ).left.map(msg => new Exception(s"Answer error: $msg"))

  }

}
