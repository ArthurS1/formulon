package fr.konexii.form.application.utils

import cats._
import cats.data._
import cats.syntax.all._

/*
 * An exception that can concatenate any number of exceptions.
 * Intended for usage with ValidatedNecs.
 */
final case class CompositeException(exceptions: NonEmptyChain[Throwable])
    extends Exception {

  override def getMessage(): String =
    "Multiple errors were caught : " + exceptions
      .map(_.getMessage)
      .toList
      .mkString(", ")

}

