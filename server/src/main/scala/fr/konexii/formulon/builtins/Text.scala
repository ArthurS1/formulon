package fr.konexii.formulon.builtins

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.application.Casts._

import cats.syntax.all._
import cats.data._

import io.circe._
import io.circe.syntax._

// TODO: Still a lot of boilerplate to remove around here.

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Exceptions//////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

sealed trait TextException extends KeyedException

sealed case class TextSizeOutOfBount(actualSize: Int) extends TextException
sealed case class ZipperFocusOnEnd(history: List[ZipperHistory])
    extends TextException
sealed case class DecodingFailure(message: String) extends TextException

object Conversion {
  def toKeyedWithMessage(e: TextException): KeyedExceptionWithMessage =
    e match {
      case v @ TextSizeOutOfBount(actualSize) =>
        KeyedExceptionWithMessage.fromKeyedException(
          v,
          s"Text size is out of bound. Actual size $actualSize."
        )
      case v @ ZipperFocusOnEnd(history) =>
        KeyedExceptionWithMessage.fromKeyedException(
          v,
          s"Zipper focus was on an end node when trying to decode. $history"
        )
      case v @ DecodingFailure(message) =>
        KeyedExceptionWithMessage.fromKeyedException(
          v,
          s"Decoding failure ($message)."
        )
    }
}

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////Plugin/////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

final case class Text() extends Plugin {

  val name = Text.name

  def validate: Validator.Validation[KeyedExceptionWithMessage] = z =>
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
      case _ =>
        Left(
          NonEmptyChain.one(
            Conversion.toKeyedWithMessage(ZipperFocusOnEnd(z.history))
          )
        )
    }

  // This should be what the end plugin developer should use
  private def validate_(
      f: TextField,
      a: TextAnswer,
      zipper: Zipper[Validator.Association]
  ): ValidatedNec[KeyedExceptionWithMessage, Zipper[Validator.Association]] =
    if (a.value.length() < f.maxLength && a.value.length() >= f.minLength)
      Conversion.toKeyedWithMessage(TextSizeOutOfBount(a.value.length())).invalidNec
    else
      zipper.next.left
        .map(f =>
            Conversion.toKeyedWithMessage(ZipperFocusOnEnd(f.history))
        )
        .toValidatedNec

  import TextField._
  import TextAnswer._

  def serializeField(field: Field): Either[KeyedExceptionWithMessage, Json] =
    field.to[TextField](Text.name).map(_.asJson)

  def deserializeField(field: Json): Either[KeyedExceptionWithMessage, Field] =
    field
      .as[TextField]
      .left
      .map(err => Conversion.toKeyedWithMessage(DecodingFailure(err.message)))

  def serializeAnswer(answer: Answer): Either[KeyedExceptionWithMessage, Json] =
    answer.to[TextAnswer](Text.name).map(_.asJson)

  def deserializeAnswer(
      answer: Json
  ): Either[KeyedExceptionWithMessage, Answer] =
    answer
      .as[TextAnswer]
      .left
      .map(err => Conversion.toKeyedWithMessage(DecodingFailure(err.message)))

}

object Text {

  val name = "text"

}

////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////Field////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

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

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////Answer//////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

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
