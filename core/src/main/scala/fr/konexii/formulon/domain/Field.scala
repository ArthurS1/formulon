package fr.konexii.formulon.domain

import cats.data._
import cats.syntax.all._

import fr.konexii.formulon.domain.Invariants._

trait Field extends Named

final case class FieldWithMetadata(
    title: String,
    required: Boolean,
    field: Field
)

object FieldWithMetadata {

  def apply(
      title: String,
      required: Boolean,
      field: Field
  ): ValidatedNec[InvariantsException, FieldWithMetadata] =
    (validateTitle(title)).map(new FieldWithMetadata(_, required, field))

  def validateTitle(title: String): ValidatedNec[InvariantsException, String] =
    isNotBlank(title) *> isNotMoreThan(80, title) *> isOnlyUnicodeLetters(title)

}
