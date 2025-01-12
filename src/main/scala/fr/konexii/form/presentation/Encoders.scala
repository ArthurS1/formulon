package fr.konexii.form
package presentation

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import io.circe.JsonObject
import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.domain.SchemaTree._

sealed abstract private[presentation] class EntityCirceInstances[
    T: Encoder: Decoder
] {

  implicit val encoderForEntity: Encoder[Entity[T]] = new Encoder[Entity[T]] {
    def apply(a: Entity[T]): Json = Json.obj(
      (a.id.toString, a.data.asJson)
    )
  }

  // TODO : Missing decoder

}

private final case class Augumented[T](
    data: T,
    id: UUID,
    nextId: Option[UUID],
    blockLevelType: String
)

sealed abstract private[presentation] class SchemaTreeCirceInstances[
    T: Encoder: Decoder
] extends EntityCirceInstances[T] {

  implicit val encoderForSchemaTree: Encoder[SchemaTree[T]] =
    new Encoder[SchemaTree[T]] {

      def apply(st: SchemaTree[T]): Json = {
        val augumented: SchemaTree[Augumented[T]] = augument(st)
        /* TODO:
         * These should be rebuilt once the Traversable instance is done,
         * would allow to return better errors probably.
         */
        val entityEncoded: SchemaTree[Option[Json]] =
          augumented.map(encode)

        entityEncoded.foldl[Json](Json.obj()) {
          case (acc, Some(elem)) =>
            acc.deepMerge(elem)
          case (acc, None) => acc
        }
      }

      private def encode(agmt: Augumented[T]): Option[Json] =
        HCursor
          .fromJson(agmt.data.asJson)
          .downField(agmt.id.toString)
          .withFocus(json => {
            val blockLevelFields = List(
              ("type", Json.fromString(agmt.blockLevelType)),
              ("data", json)
            )
            val nextField =
              if (agmt.nextId.isDefined)
                List(("next", Json.fromString(agmt.nextId.get.toString)))
              else List.empty

            Json.fromFields(
              blockLevelFields ++ nextField
            )
          })
          .top

      private def augument(st: SchemaTree[T]): SchemaTree[Augumented[T]] =
        st match {
          case Block(
                Entity(id, data),
                next @ Some(Block(Entity(nextId, _), _))
              ) =>
            Block(
              Entity(id, Augumented(data, id, Some(nextId), "block")),
              next.map(augument(_))
            )
          case Block(
                Entity(id, data),
                None
              ) =>
            Block(
              Entity(id, Augumented(data, id, None, "block")),
              None
            )
          case Branch(choices) =>

        }
    }

  implicit val decoderForSchemaTree: Decoder[SchemaTree[T]] =
    new Decoder[SchemaTree[T]] {

      def apply(
          c: HCursor
      ): Decoder.Result[SchemaTree[T]] =
        for {
          listOfKeys <- Either.fromOption(
            c.keys.map(_.toList),
            DecodingFailure("No keys in the root object", c.history)
          )
          listOfUuids <- listOfKeys
            .map(key =>
              Either
                .catchNonFatal(UUID.fromString(key))
                .left
                .map((err: Throwable) =>
                  DecodingFailure(err.getMessage(), c.history)
                )
            )
            .sequence
          result <- decodeSingleEntity(listOfUuids.head, listOfUuids.tail, c)
        } yield result

      private def decodeSingleEntity(
          current: UUID,
          rest: List[UUID],
          c: HCursor
      ): Decoder.Result[SchemaTree[T]] = {
        val st = c.downField(current.toString)
        val field = st.downField("field")
        val stType = st.downField("type")

        stType
          .as[String]
          .flatMap(_ match {
            case "block" => {
              val entity =
                field.as[T].map(f => Entity(current, f))
              val next = st.downField("next").as[UUID]
              entity.flatMap(e =>
                next match {
                  case Left(_) => Right(Block(e, None))
                  case Right(uuid) =>
                    decodeSingleEntity(uuid, rest.filter(_ != next), c)
                }
              )
            }
          })
      }

    }
}
