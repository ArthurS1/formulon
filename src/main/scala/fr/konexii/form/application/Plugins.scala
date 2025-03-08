package fr.konexii.form.application

import cats.data._
import cats.syntax.all._

import io.circe._

trait Plugin[A <: field_.Field, B <: answer_.Answer] {

  /*
   * Decodes the inside of the data field of the schema in JSON form.
   * This function will be called whenever the `typeName` of a schema field
   * matches your plugin `typeName`.
   */
  implicit val fieldDecoder: Decoder[A]

  /*
   * Encodes the inside of the data field of the schema in JSON form.
   */
  implicit val fieldEncoder: Encoder[A]

  implicit val answerDecoder: Decoder[B]
  implicit val answerEncoder: Encoder[B]

  def typeName: String

  def validate(f: A, a: B): ValidatedNec[Throwable, B]

}

object Plugin {

  def decodeField[A <: field_.Field](
      typeName: String,
      data: Json,
      plugin: NonEmptyList[Plugin[A, _]]
  ): Either[DecodingFailure, field_.Field] =
    for {
      plugin <- Either.fromOption(
        plugin
          .find(plugin => plugin.typeName === typeName),
        DecodingFailure(s"Failed to find a plugin for field of type $typeName", List())
      )
      field <- plugin.fieldDecoder.decodeJson(data)
    } yield field

  def decodeAnswer(
      typeName: String,
      data: Json,
      plugin: NonEmptyList[Plugin[field_.Field, answer_.Answer]]
  ): Either[DecodingFailure, answer_.Answer] =
    for {
      plugin <- Either.fromOption(
        plugin
          .find(plugin => plugin.typeName === typeName),
        DecodingFailure(s"Failed to find a plugin for field of type $typeName", List())
      )
      answer <- plugin.answerDecoder.decodeJson(data)
    } yield answer

}

package field_ {
  trait Field
  final case class Text() extends Field
  final case class MultipleChoice() extends Field
}

package answer_ {
  trait Answer
  final case class Text(value: String) extends Answer
  final case class MultipleChoice(value: String) extends Answer
}

object A {


  val textPlugin: Plugin[_, _] = new Plugin[field_.Text, answer_.Text] {

    implicit val fieldDecoder: Decoder[field_.Text] =
      new Decoder[field_.Text] {
        def apply(c: HCursor): Decoder.Result[field_.Text] = Right(
          field_.Text()
        )
      }

    implicit val fieldEncoder: Encoder[field_.Text] =
      new Encoder[field_.Text] {
        def apply(a: field_.Text): Json = Json.obj()
      }

    implicit val answerDecoder: Decoder[answer_.Text] =
      new Decoder[answer_.Text] {
        def apply(c: HCursor): Decoder.Result[answer_.Text] =
          for {
            value <- c.downField("value").as[String]
          } yield answer_.Text(value)
      }

    implicit val answerEncoder: Encoder[answer_.Text] =
      new Encoder[answer_.Text] {
        def apply(a: answer_.Text): Json = Json.obj(
          ("value", Json.fromString(a.value))
        )
      }

    val typeName = "text"

    def validate(
        f: field_.Text,
        a: answer_.Text
    ): ValidatedNec[Throwable, answer_.Text] = a.validNec

  }

  val registry: NonEmptyList[Plugin[_, _]] = NonEmptyList(textPlugin, List())

}
