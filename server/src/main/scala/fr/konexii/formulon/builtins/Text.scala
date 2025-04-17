package fr.konexii.formulon.builtins.Text

import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.domain._

import cats.syntax.all._
import cats.data._

import io.circe._
import io.circe.syntax._

import shapeless.Typeable
import shapeless.syntax.typeable._

/* TODO :
 *  - add invariant logic
 *  - try out the thing
 */

object Utils {

  def unwrapTypes[F <: Field: Typeable, A <: Answer: Typeable](
      field: Field,
      answer: Answer,
      g: (F, A) => ValidatedNec[Throwable, (Field, Answer)]
  ): ValidatedNec[Throwable, (Field, Answer)] = if (
    field.name === Text.name && answer.name === Text.name
  ) (field.cast[F], answer.cast[A]) match {
    case (Some(f), Some(a)) => g(f, a)
    case _                  => new Exception("Could not safely cast").invalidNec
  }
  else
    new Exception("Field type is different from answer type").invalidNec

}

final case class Text() extends Plugin {

  import Utils._

  val name = Text.name

  def validate(
      field: Field,
      answer: Answer
  ): ValidatedNec[Throwable, (Field, Answer)] =
    unwrapTypes(
      field,
      answer,
      (f: TextField, a: TextAnswer) =>
        if (a.value.length() < f.maxLength && a.value.length() >= f.minLength)
          (new Exception("Custom exception")).invalidNec
        else (f, a).validNec
    )

  import TextField._
  import TextAnswer._

  def serializeField(field: Field): Either[Throwable, Json] = Either.fromOption(
    field.cast[TextField].map(_.asJson),
    new Exception("casting failed probably")
  )

  def deserializeField(field: Json): Either[Throwable, Field] =
    field.as[TextField].left.map(err => new Exception(err.message))

  def serializeAnswer(answer: Answer): Either[Throwable, Json] = Either.fromOption(
    answer.cast[TextAnswer].map(_.asJson),
    new Exception("casting failed probably")
  )

  def deserializeAnswer(answer: Json): Either[Throwable, Answer] =
    answer.as[TextAnswer].left.map(err => new Exception(err.message))

}

object Text {

  val name = "text"

}

final case class TextField(val maxLength: Int, val minLength: Int)
    extends Field {

  val name = Text.name

}

object TextField {

  implicit val encoderForTextField: Encoder[TextField] =
    new Encoder[TextField] {
      def apply(a: TextField): Json =
        Json.obj(
          ("maxLength", Json.fromInt(a.maxLength)),
          ("minLength", Json.fromInt(a.minLength))
        )
    }

  implicit val decoderForTextField: Decoder[TextField] =
    new Decoder[TextField] {
      def apply(c: HCursor): Decoder.Result[TextField] =
        for {
          maxLength <- c.downField("maxLength").as[Int]
          minLength <- c.downField("minField").as[Int]
        } yield TextField(maxLength, minLength)
    }

}

final case class TextAnswer(val value: String) extends Answer {

  val name = Text.name

}

object TextAnswer {

  implicit val encoderForTextAnswer: Encoder[TextAnswer] =
    new Encoder[TextAnswer] {
      def apply(a: TextAnswer): Json =
        Json.obj(("value", Json.fromString(a.value)))
    }

  implicit val decoderForTextAnswer: Decoder[TextAnswer] =
    new Decoder[TextAnswer] {
      def apply(c: HCursor): Decoder.Result[TextAnswer] =
        for {
          value <- c.downField("value").as[String]
        } yield TextAnswer(value)
    }

}
