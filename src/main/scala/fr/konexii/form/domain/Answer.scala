package fr.konexii.form.domain.answer

import io.circe._
import io.circe.syntax._
import io.circe.parser.decode
import io.circe.Decoder.decodeList

import fr.konexii.form.domain._

/*
 * This structure is created with the idea that in the future we might want to
 * attach more data to a the client's list of answers (like timestamp or custom data).
 */
final case class Submission(
    answers: List[Entity[Answer]]
)

sealed trait Answer {}

final case class Text(value: String) extends Answer

object Text extends TextInstances

sealed abstract private[answer] class TextInstances {

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
