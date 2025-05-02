package fr.konexii.formulon.application

import io.circe._

import fr.konexii.formulon.domain._

/*
 * In this interface, everything is abstracted by Field and Answer
 * When we actually use those functions though, they safely cast the
 * inputs to the desired concrete type thanks to shapeless (?).
 * Thus returning options or errors.
 */
trait Plugin {
  def name: String

  def validate: Validator.Validation

  def serializeField(field: Field): Either[ValidatorException, Json]

  def deserializeField(field: Json): Either[ValidatorException, Field]

  def serializeAnswer(answer: Answer): Either[ValidatorException, Json]

  def deserializeAnswer(answer: Json): Either[ValidatorException, Answer]
}
