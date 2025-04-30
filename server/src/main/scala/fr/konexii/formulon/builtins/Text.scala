package fr.konexii.formulon.builtins

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.application.Casts._

import cats.syntax.all._
import cats.data._

import io.circe._
import io.circe.syntax._

final case class Text() extends Plugin {

  val name = Text.name

  def validate(
      z: Zipper[Validator.Association]
  ): Either[NonEmptyChain[Throwable], Zipper[Validator.Association]] =
    z.focus match {
      case Trunk(Entity(id, (Some(answer), fieldWithMetadata)), _) =>
        for {
          a <- answer
            .to[TextAnswer](Text.name)
            .left
            .map(NonEmptyChain.one(_))
          f <- fieldWithMetadata.field
            .to[TextField](Text.name)
            .left
            .map(NonEmptyChain.one(_))
          result <- validate_(f, a, z).toEither
        } yield result
      case _ => Left(NonEmptyChain.one(new Exception("Unexpected")))
    }

  // This should be what the end plugin developer should use
  private def validate_(
      f: TextField,
      a: TextAnswer,
      zipper: Zipper[Validator.Association]
  ): ValidatedNec[Throwable, Zipper[Validator.Association]] =
    if (a.value.length() < f.maxLength && a.value.length() >= f.minLength)
      (new Exception("Custom exception")).invalidNec
    else zipper.next.left.map(f => new Exception(f.msg)).toValidatedNec

  import TextField._
  import TextAnswer._

  def serializeField(field: Field): Either[Throwable, Json] =
    field.to[TextField](Text.name).map(_.asJson)

  def deserializeField(field: Json): Either[Throwable, Field] =
    field.as[TextField].left.map(err => new Exception(err.message))

  def serializeAnswer(answer: Answer): Either[Throwable, Json] =
    answer.to[TextAnswer](Text.name).map(_.asJson)

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
