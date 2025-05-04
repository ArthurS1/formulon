package fr.konexii.formulon.domain

import cats.syntax.all._

import cats.data._

sealed trait InvariantsException
sealed case class Empty() extends InvariantsException
sealed case class TooLong(nbMaxChar: Int) extends InvariantsException
sealed case class NotAlphaNumeric() extends InvariantsException
sealed case class NotUnicodeLetters() extends InvariantsException

object Invariants {

  def isNotBlank(s: String): ValidatedNec[InvariantsException, String] =
    if (s.isEmpty()) Empty().invalidNec else s.validNec

  def isNotMoreThan(nbChar: Int, s: String): ValidatedNec[InvariantsException, String] =
    if (s.size > nbChar)
      TooLong(nbChar).invalidNec
    else s.validNec

  def isOnlyAlphasAndDigits(s: String): ValidatedNec[InvariantsException, String] =
    if (s.matches("([A-Za-z0-9 ])*"))
      s.validNec
    else
      NotAlphaNumeric().invalidNec

  def isOnlyUnicodeLetters(s: String): ValidatedNec[InvariantsException, String] =
    if (s.matches("\\p{L}*"))
      s.validNec
    else
      NotUnicodeLetters().invalidNec

}
