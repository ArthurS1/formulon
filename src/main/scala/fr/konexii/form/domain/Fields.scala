package fr.konexii.form.domain.fields

import io.circe._
import io.circe.syntax._

final case class FieldWithMetadata(
    title: String,
    required: Boolean,
    field: Field
)

sealed trait Field

/*
 * TODO : This will be moved when setting up the plugin system.
 */

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
