package fr.konexii.form
package domain

import cats._
import cats.syntax.all._

import io.circe.{Decoder, Encoder}
import io.circe.Json
import io.circe.HCursor
import io.circe.DecodingFailure
import java.util.UUID

/*
 * Example of input/output jsons.
 *
 * "<uuid>" {
 *  "type": "end"
 * }
 * "<uuid>" {
 *  "type": "branch"
 *  "cond": "<uuid>.value == "test""
 *  "ifTrue": <uuid>
 *  "ifFalse": <uuid>
 * }
 * "<uuid>" {
 *  "type": "text"
 *  "title": "hello"
 *  "required": true
 *  "tooltip": "please insert hello"
 * ...
 *  "next": <uuid>
 * }
 */

sealed trait Block

object Block extends BlockInstances

final case class End() extends Block

sealed abstract private[domain] class BlockInstances {

  implicit val encoderForBlockEntity: Encoder[Entity[Block]] =
    new Encoder[Entity[Block]] {
      def apply(entity: Entity[Block]): Json = entity.data match {
        case End() =>
          Json.obj(
            (
              entity.id.toString,
              Json.obj(
                ("type", Json.fromString("end"))
              )
            )
          )
      }
    }

  implicit val decoderForBlockEntity: Decoder[Entity[Block]] =
    new Decoder[Entity[Block]] {
      def apply(c: HCursor): Decoder.Result[Entity[Block]] = {

        /* Following is trash and only works with End */

        val uuid = c.keys.map(_.head).get
        val uuidField = c.downField(uuid)
        val blockTypeField = uuidField.downField("type")

        for {
          blockType <- blockTypeField.as[String]
          parsedUUID <- Either
            .catchNonFatal(UUID.fromString(uuid))
            .left
            .map((err: Throwable) =>
              DecodingFailure(err.getMessage, uuidField.history)
            )
          block <- blockType match {
            case "end" => Right(Entity[Block](parsedUUID, End()))
            case _ =>
              Left(
                DecodingFailure(
                  "Block type is unknown",
                  blockTypeField.history
                )
              )
          }
        } yield block
      }
    }

}
