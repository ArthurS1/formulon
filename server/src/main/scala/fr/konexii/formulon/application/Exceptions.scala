package fr.konexii.formulon.application

import cats.data._
import cats.syntax.all._

/*
 * An exception that can concatenate any number of exceptions.
 * Intended for usage with ValidatedNecs.
 */
final case class CompositeException(exceptions: NonEmptyChain[String])
    extends Exception {

  override def getMessage(): String =
    if (exceptions.size === 1)
      "An error was caught : " + exceptions.toList
        .mkString(", ")
    else
      "Multiple errors were caught : " + exceptions.toList
        .mkString(", ")

}

final case class UnauthorizedException(message: String)
    extends Exception(message) {}
