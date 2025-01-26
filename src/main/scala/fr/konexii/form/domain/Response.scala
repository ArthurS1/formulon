package fr.konexii.form
package domain
package response

import io.circe._
import io.circe.syntax._

sealed trait Response

final case class Text(value: String)

object Text extends TextInstances

sealed abstract private[response] class TextInstances {

  implicit val decoderForText: Decoder[Text] =
    new Decoder[Text] {
      def apply(c: HCursor): Decoder.Result[Text] =
        for {
          value <- c.downField("value").as[String]
        } yield Text(value)
    }

  implicit val encoderForText: Encoder[Text] =
    new Encoder[Text] {
      def apply(a: Text): Json = Json.obj(
        ("value", Json.fromString(a.value))
      )
    }

}
