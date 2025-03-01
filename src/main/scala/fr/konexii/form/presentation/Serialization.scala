package fr.konexii.form.presentation

import cats.syntax.all._

import io.circe._
import io.circe.syntax._

import java.util.UUID

import scala.util.Try

import fr.konexii.form.domain._
import fr.konexii.form.domain.answer._
import fr.konexii.form.domain.field._
import fr.konexii.form.application._

object Serialization
    extends EntityCirceInstances
    with AnswerCirceInstances
    with SubmissionCirceInstances
    with FieldWithMetadataCirceInstances
    with SchemaTreeCirceInstances

sealed trait FieldWithMetadataCirceInstances {

  def encoderForFieldWithMetadata(
      c: Component[Field, Answer]
  ): Encoder[FieldWithMetadata] =
    new Encoder[FieldWithMetadata] {
      def apply(a: FieldWithMetadata): Json = {
        Json.obj(
          ("type", Json.fromString(c.typeStr)),
          ("data", a.field.asJson(c.encoderForField)),
          ("title", Json.fromString(a.title)),
          ("required", Json.fromBoolean(a.required))
        )
      }
    }

  implicit val decoderForFieldWithMetadata: Decoder[FieldWithMetadata] =
    new Decoder[FieldWithMetadata] {
      def apply(c: HCursor): Decoder.Result[FieldWithMetadata] = {
        val type_ = c.downField("type")

        for {
          fieldType <- type_.as[String]
          component <- Either.fromOption(
            Component.forType(fieldType),
            DecodingFailure(s"Type \"$fieldType\" is unknown", c.history)
          )
          field <- c.downField("data").as[Field](component.decoderForField)
          title <- c.downField("title").as[String]
          required <- c.downField("required").as[Boolean]
        } yield FieldWithMetadata(title, required, field)
      }
    }

}

sealed trait AnswerCirceInstances {

  implicit def decoderForAnswer: Decoder[Answer] =
    new Decoder[Answer] {
      def apply(c: HCursor): Decoder.Result[Answer] =
        for {
          responseType <- c.downField("type").as[String]
          component <- Either.fromOption(
            Component.forType(responseType),
            DecodingFailure("No such response type.", c.history)
          )
          result <- c.downField("data").as[Answer](component.decoderForAnswer)
        } yield result
    }

  def encoderForAnswer(c: Component[Field, Answer]): Encoder[Answer] =
    new Encoder[Answer] {
      def apply(a: Answer): Json =
        Json.obj(
          ("type", Json.fromString(c.typeStr)),
          ("data", a.asJson(c.encoderForAnswer))
        )
    }
}

sealed trait SubmissionCirceInstances {

  import fr.konexii.form.presentation.Serialization._

  implicit def decoderForSubmission: Decoder[Submission] =
    new Decoder[Submission] {
      def apply(c: HCursor): Decoder.Result[Submission] =
        for {
          answers <- c
            .downField("answers")
            .as[List[Entity[Answer]]]
        } yield Submission(answers)
    }

  implicit def encoderForSubmission: Encoder[Submission] =
    new Encoder[Submission] {
      def apply(a: Submission): Json = Json.obj(
        ("answers", a.answers.asJson)
      )
    }

}

sealed trait EntityCirceInstances {

  implicit def encoderForEntity[T: Encoder]: Encoder[Entity[T]] =
    new Encoder[Entity[T]] {
      def apply(a: Entity[T]): Json =
        Json.obj((a.id.toString, Json.obj(("data", a.data.asJson))))
    }

  // in an object, will attempt to decode the entity with given id
  def decoderForEntityWithUuid[T: Decoder](uuid: UUID): Decoder[Entity[T]] =
    new Decoder[Entity[T]] {
      def apply(c: HCursor): Decoder.Result[Entity[T]] = {
        for {
          data <- c
            .downField(uuid.toString)
            .downField("data")
            .as[T]
        } yield Entity(uuid, data)
      }
    }

  // In an object, will attempt to decode the first entity found
  implicit def decoderForEntity[T: Decoder]: Decoder[Entity[T]] =
    new Decoder[Entity[T]] {
      def apply(c: HCursor): Decoder.Result[Entity[T]] = {
        for {
          key <- Either.fromOption(
            c.keys.flatMap(keys =>
              keys.foldLeft[Option[UUID]](None) {
                case (None, e) => Try(UUID.fromString(e)).toOption
                case (v, _)    => v
              }
            ),
            DecodingFailure(
              "Could not find a key with a valid UUID in the object.",
              c.history
            )
          )
          result <- decoderForEntityWithUuid[T](key).apply(c)
        } yield result
      }
    }
}

sealed trait SchemaTreeCirceInstances {

  import Serialization.encoderForEntity

  implicit def encoderForSchemaTree[A: Encoder]
      : Encoder[SchemaTree[Entity[A]]] =
    new Encoder[SchemaTree[Entity[A]]] {
      def apply(a: SchemaTree[Entity[A]]): Json = a match {
        case Branch(content, next, out) =>
          content.asJson
            .deepMerge(nextId(content.id, "next", next))
            .deepMerge(nextId(content.id, "out", out))
            .deepMerge(apply(next))
            .deepMerge(apply(out))
        case Trunk(content, next) =>
          content.asJson
            .deepMerge(nextId(content.id, "next", next))
            .deepMerge(apply(next))
        case End() => Json.obj()
      }

      private def nextId(
          nodeId: UUID,
          fieldName: String,
          st: SchemaTree[Entity[A]]
      ): Json =
        st.id
          .map(id =>
            Json.obj(
              (
                nodeId.toString,
                Json.obj((fieldName, Json.fromString(id.toString)))
              )
            )
          )
          .getOrElse(Json.obj())
    }

  implicit def decoderForSchemaTree[A: Decoder]
      : Decoder[SchemaTree[Entity[A]]] =
    new Decoder[SchemaTree[Entity[A]]] {
      def apply(c: HCursor): Decoder.Result[SchemaTree[Entity[A]]] =
        for {
          firstEntity <- Either.fromOption(
            c.keys.flatMap(_.toList.lastOption),
            DecodingFailure("No records in the JSON object.", c.history)
          )
          uuid <- Either
            .catchNonFatal(UUID.fromString(firstEntity))
            .left
            .map(t => DecodingFailure(t.getMessage, c.history))
          result <- decodeNext(c, uuid)
        } yield result

      private def decodeNext(
          c: HCursor,
          currentId: UUID
      ): Decoder.Result[SchemaTree[Entity[A]]] = {
        for {
          entity <- c.as[Entity[A]](
            Serialization.decoderForEntityWithUuid(currentId)
          )
          entityCursor = c.downField(currentId.toString)
          nextUuids = (
            entityCursor.downField("out").as[UUID],
            entityCursor.downField("next").as[UUID]
          )
          result <- nextUuids match {
            case (Right(out), Right(next)) =>
              for {
                next <- decodeNext(c, next)
                out <- decodeNext(c, out)
              } yield Branch(entity, next, out)
            case (_, Right(next)) =>
              for {
                next <- decodeNext(c, next)
              } yield Trunk(entity, next)
            case (Left(_), Left(_))    => Right(Trunk(entity, End()))
            case (Right(_), Left(err)) => Left(err)
          }
        } yield result
      }
    }
}
