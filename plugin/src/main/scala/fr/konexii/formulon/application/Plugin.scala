package fr.konexii.formulon.application

import io.circe._

import cats.data.NonEmptyChain
import fr.konexii.formulon.domain._

/*
 * In this interface, everything is abstracted by Field and Answer
 * When we actually use those functions though, they safely cast the
 * inputs to the desired concrete type thanks to shapeless (?).
 * Thus returning options or errors.
 */
trait Plugin {
  def name: String

  def validate(z: Zipper[Validator.Association]): Either[
      NonEmptyChain[Throwable],
      Zipper[Validator.Association]
    ]

  def serializeField(field: Field): Either[Throwable, Json]

  def deserializeField(field: Json): Either[Throwable, Field]

  def serializeAnswer(answer: Answer): Either[Throwable, Json]

  def deserializeAnswer(answer: Json): Either[Throwable, Answer]
}
