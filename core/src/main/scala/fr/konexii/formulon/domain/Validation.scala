package fr.konexii.formulon.domain

import cats.syntax.all._

import cats.data._

sealed trait ValidationException
sealed case class Empty() extends ValidationException
sealed case class TooLong(nbMaxChar: Int) extends ValidationException
sealed case class NotAlphaNumeric() extends ValidationException
sealed case class NotUnicodeLetters() extends ValidationException

object Validation {

  def isNotBlank(s: String): ValidatedNec[ValidationException, String] =
    if (s.isEmpty()) Empty().invalidNec else s.validNec

  def isNotMoreThan(nbChar: Int, s: String): ValidatedNec[ValidationException, String] =
    if (s.size > nbChar)
      TooLong(nbChar).invalidNec
    else s.validNec

  def isOnlyAlphasAndDigits(s: String): ValidatedNec[ValidationException, String] =
    if (s.matches("([A-Za-z0-9 ])*"))
      s.validNec
    else
      NotAlphaNumeric().invalidNec

  def isOnlyUnicodeLetters(s: String): ValidatedNec[ValidationException, String] =
    if (s.matches("\\p{L}*"))
      s.validNec
    else
      NotUnicodeLetters().invalidNec

}
