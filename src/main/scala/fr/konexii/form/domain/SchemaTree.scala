package fr.konexii.form
package domain

import cats._
import cats.syntax.all._

/*
 * Example of input/output jsons.
 *
 * "<uuid>" {
 *  "type": "branch"
 *  "cond": "<uuid>.value == "test""
 *  "ifTrue": <uuid>
 *  "ifFalse": <uuid>
 * }
 * "<uuid>" {
 *  "type": "block"
 *  "field": {
 *    "type": "text"
 *    "title": "hello"
 *    "required": true
 *    "data" {
 *      "tooltip": "please insert hello"
 *      ...
 *    }
 *  }
 *  "next": <uuid> (optional if it does not exist, this is the end of the chain)
 * }
 */

sealed trait SchemaTree[T]

final case class Block[T](data: Entity[T], next: Option[SchemaTree[T]])
    extends SchemaTree[T]
final case class Branch[T](
    choices: Entity[(Option[SchemaTree[T]], Option[SchemaTree[T]])]
) extends SchemaTree[T]

object SchemaTree extends SchemaTreeInstances

sealed abstract private[domain] class SchemaTreeInstances {

  implicit val functorForSchemaTree: Functor[SchemaTree] =
    new Functor[SchemaTree] {
      def map[A, B](fa: SchemaTree[A])(f: A => B): SchemaTree[B] = fa match {
        case Block(data, next) => Block(data.map(f(_)), next.map(_.map(f)))
        case Branch(choices) =>
          Branch(choices.map { case (left, right) =>
            (left.map(_.map(f)), right.map(_.map(f)))
          })
      }
    }

  implicit val foldableForSchemaTree: Foldable[SchemaTree] =
    new Foldable[SchemaTree] {
      def foldLeft[A, B](fa: SchemaTree[A], b: B)(f: (B, A) => B): B =
        fa match {
          case Block(Entity(_, data), None) => f(b, data)
          case Block(Entity(_, data), Some(next)) =>
            foldLeft(next, f(b, data))(f)
          case Branch(Entity(_, (Some(left), Some(right)))) => {
            val a = foldLeft(left, b)(f)
            foldLeft(right, a)(f)
          }
          case Branch(Entity(_, (Some(left), None))) =>
            foldLeft(left, b)(f)
          case Branch(Entity(_, (None, Some(right)))) =>
            foldLeft(right, b)(f)
          case Branch(_) =>
            b
        }

      def foldRight[A, B](fa: SchemaTree[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] =
        fa match {
          case Block(Entity(_, data), None)       => f(data, lb)
          case Block(Entity(_, data), Some(next)) => foldRight(next, f(data, lb))(f)
          case Branch(Entity(_, (Some(left), Some(right)))) => {
            val a = foldRight(right, lb)(f)
            foldRight(left, a)(f)
          }
          case Branch(Entity(_, (Some(left), None))) =>
            foldRight(left, lb)(f)
          case Branch(Entity(_, (None, Some(right)))) =>
            foldRight(right, lb)(f)
          case Branch(_) =>
            lb
        }
    }

}

/*
object SchemaTreeOfEntityOfFieldWithMetadata
    extends SchemaTreeOfEntityInstances[FieldWithMetadata]

sealed abstract private[domain] class SchemaTreeOfEntityInstances[
    T: Encoder: Decoder
] {

  implicit val encoderForSchemaTree: Encoder[SchemaTree[Entity[T]]] =
    new Encoder[SchemaTree[Entity[T]]] {

      type JsonTuple = (String, Json)

      def apply(st: SchemaTree[Entity[T]]): Json = {
        val entityEncoded: SchemaTree[JsonTuple] = encodeSingleEntity(st)
        val jsonList: List[JsonTuple] =
          entityEncoded.foldl[List[JsonTuple]](List())(
            (acc: List[JsonTuple], elem: JsonTuple) => elem :: acc
          )
        Json.fromFields(jsonList)
      }

      private def encodeSingleEntity(
          st: SchemaTree[Entity[T]]
      ): SchemaTree[JsonTuple] =
        st match {
          case Block(Entity(id, data), None) =>
            Block(
              (
                id.toString,
                Json.obj(
                  ("type", Json.fromString("block")),
                  ("field", data.asJson)
                )
              ),
              None
            )
          case Block(entity, Some(nextObj @ Block(Entity(idNext, _), _))) =>
            Block(
              (
                entity.id.toString,
                Json.obj(
                  ("type", Json.fromString("block")),
                  ("next", Json.fromString(idNext.toString)),
                  ("field", entity.data.asJson)
                )
              ),
              Some(encodeSingleEntity(nextObj))
            )
          case Block(entity, Some(Branch(left, right))) =>
            Block(
              (
                entity.id.toString,
                Json.obj(
                  ("type", Json.fromString("block")),
                  ("next", Json.fromString(idNext.toString)),
                  ("field", entity.data.asJson)
                )
              ),
              Some(encodeSingleEntity(nextObj))
            )
          case Branch(left, right) =>
            Branch(
              left.map(st => encodeSingleEntity(st)),
              right.map(st => encodeSingleEntity(st))
            )
        }

    }

  implicit val decoderForSchemaTree: Decoder[SchemaTree[Entity[T]]] =
    new Decoder[SchemaTree[Entity[T]]] {

      def apply(
          c: HCursor
      ): Decoder.Result[SchemaTree[Entity[T]]] =
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
          rest: List[UUID], // TODO : maybe rest should be a Set ?
          c: HCursor
      ): Decoder.Result[SchemaTree[Entity[T]]] = {
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
*/
