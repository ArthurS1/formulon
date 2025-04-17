package fr.konexii.formulon.presentation

import cats.syntax.all._

import io.circe._
import io.circe.Encoder._
import io.circe.syntax._
import io.circe.generic.semiauto._

import java.util.UUID

import scala.util.Try

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.application.dtos._

object Serialization
    extends EntityCirceInstances
    with AnswerCirceInstances
    with SubmissionCirceInstances
    with FieldWithMetadataCirceInstances
    with TreeCirceInstances
    with BlueprintCirceInstances
    with DtosCirceInstances
    with VersionCirceInstances

sealed trait DtosCirceInstances {

  implicit val decoderForUpdateBlueprintRequest: Decoder[UpdateSchemaRequest] =
    deriveDecoder[UpdateSchemaRequest]

  implicit val decoderForCreateBlueprintRequest: Decoder[CreateSchemaRequest] =
    deriveDecoder[CreateSchemaRequest]

}

sealed trait VersionCirceInstances {

  implicit val encoderForVersion: Encoder[Version] =
    new Encoder[Version] {
      def apply(a: Version): Json =
        Json.obj(("name", Json.fromString("test"))) // TODO : remove mock data
    }

}

sealed trait BlueprintCirceInstances {

  implicit val encoderForBlueprint: Encoder[Blueprint] =
    new Encoder[Blueprint] {
      def apply(a: Blueprint): Json =
        Json.obj(("name", Json.fromString("test"))) // TODO : remove mock data
    }

}

sealed trait FieldWithMetadataCirceInstances {

  def encoderForFieldWithMetadata(
      plugins: List[Plugin]
  ): Encoder[FieldWithMetadata] =
    new Encoder[FieldWithMetadata] {
      def apply(a: FieldWithMetadata): Json = {
        val plugin = plugins.find(p => p.name === a.field.name)
        Json.obj(
          ("type", Json.fromString(a.field.name)),
          (
            "data",
            plugin.flatMap(_.serializeField(a.field).toOption) match {
              case None        => Json.fromString("error encoding")
              case Some(value) => value
            }
          ),
          ("title", Json.fromString(a.title)),
          ("required", Json.fromBoolean(a.required))
        )
      }
    }

  def decoderForFieldWithMetadata(
      plugins: List[Plugin]
  ): Decoder[FieldWithMetadata] =
    new Decoder[FieldWithMetadata] {
      def apply(c: HCursor): Decoder.Result[FieldWithMetadata] = {
        val type_ = c.downField("type")

        for {
          fieldType <- type_.as[String]
          plugin <- Either.fromOption(
            plugins.find(plugin => plugin.name === fieldType),
            DecodingFailure(
              s"Failed to find plugin for ${fieldType}",
              c.history
            )
          )
          data <- c.downField("data").as[Json]
          field <- plugin
            .deserializeField(data)
            .left
            .map(f =>
              DecodingFailure.apply(f.getMessage(), c.history)
            ) // TODO : find a way to remove this dirty fix
          title <- c.downField("title").as[String]
          required <- c.downField("required").as[Boolean]
        } yield FieldWithMetadata(title, required, field)
      }
    }

}

sealed trait AnswerCirceInstances {

  implicit def encoderForAnswer(plugins: List[Plugin]): Encoder[Answer] =
    new Encoder[Answer] {
      def apply(a: Answer): Json = {
        val plugin = plugins.find(p => p.name === a.name)

        // TODO : This could show the error message since only authorized users could read answers
        Json.obj(
          ("type", Json.fromString(a.name)),
          (
            "data",
            plugin.flatMap(_.serializeAnswer(a).toOption) match {
              case None        => Json.fromString("error encoding")
              case Some(value) => value
            }
          )
        )
      }
    }

  implicit def decoderForAnswer(plugins: List[Plugin]): Decoder[Answer] =
    new Decoder[Answer] {
      def apply(c: HCursor): Decoder.Result[Answer] =
        for {
          responseType <- c.downField("type").as[String]
          plugin <- Either.fromOption(
            plugins.find(p => p.name === responseType),
            DecodingFailure(
              s"Failed to find plugin for ${responseType}",
              c.history
            )
          )
          data <- c.downField("data").as[Json]
          result <- plugin
            .deserializeAnswer(data)
            .left
            .map(f =>
              DecodingFailure.apply(f.getMessage(), c.history)
            ) // TODO : find a way to remove this dirty fix
        } yield result
    }

}

sealed trait SubmissionCirceInstances {

  import fr.konexii.formulon.presentation.Serialization._

  implicit def decoderForSubmission(
      plugins: List[Plugin]
  ): Decoder[Submission] =
    new Decoder[Submission] {
      implicit val decoderForAnswerWithPlugins: Decoder[Answer] =
        decoderForAnswer(plugins)

      def apply(c: HCursor): Decoder.Result[Submission] =
        for {
          answers <- c
            .downField("answers")
            .as[List[Entity[Answer]]]
        } yield Submission(answers)
    }

  // TODO: We might get out of this situation by making encoders explicit and taking a JSON
  // This way we could stop the whole thing before encoding
  implicit def encoderForSubmission(
      plugins: List[Plugin]
  ): Encoder[Submission] =
    new Encoder[Submission] {
      implicit val encoderForAnswers: Encoder[List[Entity[Answer]]] =
        encodeList(encoderForEntity(encoderForAnswer(plugins)))

      def apply(a: Submission): Json = {
        Json.obj(
          ("answers", a.answers.asJson)
        )
      }
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

sealed trait TreeCirceInstances {

  import Serialization.encoderForEntity

  implicit def encoderForTree[A: Encoder]: Encoder[Tree[Entity[A]]] =
    new Encoder[Tree[Entity[A]]] {
      def apply(a: Tree[Entity[A]]): Json = a match {
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
          st: Tree[Entity[A]]
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

  implicit def decoderForTree[A: Decoder]: Decoder[Tree[Entity[A]]] =
    new Decoder[Tree[Entity[A]]] {
      def apply(c: HCursor): Decoder.Result[Tree[Entity[A]]] =
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
      ): Decoder.Result[Tree[Entity[A]]] = {
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
