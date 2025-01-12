package fr.konexii.form
package domain

import cats._
import cats.syntax.all._

import io.circe.{Decoder, Encoder, CursorOp}
import io.circe.Json
import io.circe.HCursor
import io.circe.DecodingFailure
import io.circe.syntax._
import java.util.UUID

/*
 * FieldWithMetadata properties found in all fields.
 */

final case class FieldWithMetadata(
    title: String,
    required: Boolean,
    field: Field
)

object FieldWithMetadata extends FieldWithMetadataInstances

sealed abstract private[domain] class FieldWithMetadataInstances {

  implicit val encoderForCommon: Encoder[FieldWithMetadata] =
    new Encoder[FieldWithMetadata] {
      def apply(a: FieldWithMetadata): Json = Json.obj(
        ("type", Json.fromString(Field.typeToString(a.field))),
        ("data", a.field.asJson),
        ("title", Json.fromString(a.title)),
        ("required", Json.fromBoolean(a.required))
      )
    }

  implicit val decoderForCommon: Decoder[FieldWithMetadata] =
    new Decoder[FieldWithMetadata] {
      def apply(c: HCursor): Decoder.Result[FieldWithMetadata] = {
        val type_ = c.downField("type")

        for {
          fieldType <- type_.as[String]
          data <- c.downField("data").as[Json]
          field <- Field.decoding(fieldType, data, type_.history)
          title <- c.downField("title").as[String]
          required <- c.downField("required").as[Boolean]
        } yield FieldWithMetadata(title, required, field)
      }
    }

}

/*
 * Abstraction on top of field specific data.
 * This should be the only class that needs changes when adding a new field.
 */

sealed trait Field

object Field extends FieldInstances {

  def typeToString(a: Field): String =
    a match {
      case _: Text => "text"
    }

  def decoding(
      fieldType: String,
      data: Json,
      history: List[CursorOp]
  ): Decoder.Result[Field] =
    fieldType match {
      case "text" => Decoder[Text].decodeJson(data)
      case v =>
        Left(DecodingFailure(s"Type \"$v\" is unknown", history))
    }
}

sealed abstract private[domain] class FieldInstances {

  implicit val encoderForField: Encoder[Field] =
    new Encoder[Field] {
      def apply(a: Field): Json = a match {
        case v: Text => v.asJson
      }
    }

}

/*
 * Text Field
 */

final case class Text() extends Field

object Text extends TextInstances

sealed abstract private[domain] class TextInstances {

  implicit val decoderForText: Decoder[Text] =
    new Decoder[Text] {
      def apply(c: HCursor): Decoder.Result[Text] = Right(Text())
    }

  implicit val encoderForText: Encoder[Text] =
    new Encoder[Text] {
      def apply(a: Text): Json = Json.obj()
    }

}
