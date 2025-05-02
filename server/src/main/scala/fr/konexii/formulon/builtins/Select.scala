package fr.konexii.formulon.builtins

import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.domain._

import io.circe.Json
import cats.data.NonEmptyChain

final case class Select() extends Plugin {

  def name: String = ???

  def validate: Validator.Validation = ???

  def serializeField(field: Field): Either[ValidatorException, Json] = ???

  def deserializeField(field: Json): Either[ValidatorException, Field] = ???

  def serializeAnswer(answer: Answer): Either[ValidatorException, Json] = ???

  def deserializeAnswer(answer: Json): Either[ValidatorException, Answer] = ???

}

object Select {

  val name = "select"

}

final case class SelectField(
    val selectMultiple: Boolean,
    val choices: List[String]
) extends Field() {

  val name: String = Select.name

}

final case class SelectAnswer(
    val choices: List[Int]
) extends Answer {

  val name: String = Select.name

}
