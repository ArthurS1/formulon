package fr.konexii.form
package presentation

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import io.circe.JsonObject
import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.domain.FieldWithMetadata._

object JsonUtils extends SchemaTreeCirceInstances[FieldWithMetadata]

sealed abstract private[presentation] class SchemaTreeCirceInstances[
    T: Encoder: Decoder
] {

  implicit val encoderForSchemaTree: Encoder[SchemaTree[T]] =
    new Encoder[SchemaTree[T]] {

      def apply(st: SchemaTree[T]): Json = encode(st)

      private def encode(st: SchemaTree[T]): Json =
        st match {
          case v @ Block(_, None) =>
            blockJson(v)

          case v @ Block(_, Some(next)) =>
            blockJson(v).deepMerge(encode(next))

          case v @ Branch(cond, Entity(id, (left, right))) => {
            val branch = branchJson(v)
            val b = deepMergeIfDefined(branch, left)
            deepMergeIfDefined(b, right)
          }
        }

      def blockJson(block: Block[T]): Json =
        Json
          .obj(
            (
              block.data.id.toString,
              Json.obj(
                ("type", Json.fromString("block")),
                ("field", block.data.data.asJson),
                ("next", uuidOrNull(block.next))
              )
            )
          )

      def branchJson(branch: Branch[T]): Json = {
        val Entity(_, (left, right)) = branch.choices

        Json
          .obj(
            (
              branch.choices.id.toString,
              Json.obj(
                ("type", Json.fromString("branch")),
                ("condition", Json.fromString(branch.condition)),
                ("ifTrue", uuidOrNull(left)),
                ("ifFalse", uuidOrNull(right))
              )
            )
          )
      }

      def deepMergeIfDefined(st: Json, optSt: Option[SchemaTree[T]]): Json =
        optSt match {
          case None        => st
          case Some(value) => st.deepMerge(encode(value))
        }

      def uuidOrNull(st: Option[SchemaTree[T]]): Json =
        st.map {
          case Branch(condition, Entity(id, _)) => Json.fromString(id.toString)
          case Block(Entity(id, _), next)       => Json.fromString(id.toString)
        }.getOrElse(Json.Null)

    }

  implicit val decoderForSchemaTree: Decoder[SchemaTree[T]] =
    new Decoder[SchemaTree[T]] {

      def apply(
          c: HCursor
      ): Decoder.Result[SchemaTree[T]] =
        for {
          listOfKeys <- Either.fromOption(
            c.keys.map(_.toList.reverse),
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
          result <- decode(listOfUuids.head, listOfUuids.tail.toSet, c)
        } yield result

      private def toUuidSet(uuids: List[UUID]): Either[Throwable, Set[UUID]] =
        uuids.foldl[Either[Throwable, Set[UUID]]](Right(Set.empty))(
          (set, elem) =>
            set.flatMap(s =>
              if (s.contains(elem))
                Left(new Exception(s"Found duplicate block with uuid $elem"))
              else Right(s + elem)
            )
        )

      private def decode(
          current: UUID,
          rest: Set[UUID],
          root: HCursor
      ): Decoder.Result[SchemaTree[T]] = {
        val st = root.downField(current.toString)
        val stType = st.downField("type")

        stType
          .as[String]
          .flatMap(_ match {
            case "block"  => decodeBlock(current, rest, root, st)
            case "branch" => decodeBranch(current, rest, root, st)
            case t =>
              Left(
                DecodingFailure(
                  s"Block level type \"$t\" cannot be decoded.",
                  stType.history
                )
              )
          })
      }

      private def decodeBranch(
          current: UUID,
          rest: Set[UUID],
          root: HCursor,
          c: ACursor
      ): Either[DecodingFailure, Branch[T]] = {

        def decodeOption(
            uuid: Option[UUID]
        ): Either[DecodingFailure, Option[SchemaTree[T]]] =
          uuid
            .map(next => decode(next, rest - next, root))
            .sequence
        for {
          cond <- c.downField("condition").as[String]
          leftUuid <- c.downField("ifTrue").as[Option[UUID]]
          rightUuid <- c.downField("ifFalse").as[Option[UUID]]
          left <- decodeOption(leftUuid)
          right <- decodeOption(rightUuid)
        } yield Branch(cond, Entity(current, (left, right)))
      }

      private def decodeBlock(
          current: UUID,
          rest: Set[UUID],
          root: HCursor,
          c: ACursor
      ): Either[DecodingFailure, Block[T]] = {
        val field = c.downField("field")
        val entity =
          field.as[T].map(f => Entity(current, f))
        val next = c.downField("next").as[UUID]
        entity.flatMap(e =>
          next match {
            case Left(_) => Right(Block(e, None))
            case Right(uuid) =>
              decode(uuid, rest - uuid, root)
                .flatMap(st => Right(Block(e, Some(st))))
          }
        )
      }

    }
}
