package fr.konexii.formulon.application.usecases

import cats._
import cats.data._
import cats.syntax.all._
import cats.effect.std._

import java.util.UUID

import io.circe._
import io.circe.parser.decode

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.presentation.Serialization._
import fr.konexii.formulon.domain.Validator._

class Submit[F[_]: MonadThrow: UUIDGen](
    repositories: Repositories[F],
    plugins: List[Plugin]
) {

  implicit val decoder: Decoder[Answer] = decoderForAnswer(plugins)

  def execute(
      schemaId: UUID,
      versionId: UUID,
      rawSubmission: String
  ): F[Unit] = for {
    uuid <- UUIDGen[F].randomUUID
    submission <- MonadThrow[F].fromEither(
      decode[Submission](rawSubmission)
        .map(Entity(uuid, _))
    )
    blueprint <- repositories.blueprint.get(schemaId)
    version <- MonadThrow[F].fromOption(
      blueprint.data.versions.find(e => e.id === versionId),
      new Exception(s"Failed to find version $versionId in schema $schemaId.")
    )
    _ <- MonadThrow[F].fromEither(
      Validator
        .validate(version.data.content, submission.data, validate(plugins))
        .leftMap(errors =>
          new Exception(errors.toList.map(_.getMessage()).mkString(", "))
        )
    )
    _ <- repositories.submission.create(submission, version)
  } yield ()

  private def validate(plugins: List[Plugin])(
      tree: Tree[Association]
  ): Either[NonEmptyChain[Throwable], Tree[Association]] = tree match {
    case Branch((Some(Entity(id, answer)), Entity(_, fieldWithMeta)), next, out) =>
      if (fieldWithMeta.field.name === answer.name)
        for {
          plugin <- Either
            .fromOption(
              plugins.find(p => p.name === answer.name),
              NonEmptyChain.one(
                new Exception(s"Could not find plugin for ${answer.name}")
              )
            )
          _ <- plugin.validate(fieldWithMeta.field, answer).toEither
        } yield next
      else
        Left(
          NonEmptyChain.one(
            new Exception(
              s"Type differ on $id [answer ${answer.name}] != [field ${fieldWithMeta.field.name}]"
            )
          )
        )
    case Trunk((Some(Entity(id, answer)), Entity(_, fieldWithMeta)), next) =>
      if (fieldWithMeta.field.name === answer.name)
        for {
          plugin <- Either
            .fromOption(
              plugins.find(p => p.name === answer.name),
              NonEmptyChain.one(
                new Exception(s"Could not find plugin for ${answer.name}")
              )
            )
          _ <- plugin.validate(fieldWithMeta.field, answer).toEither
        } yield next
      else
        Left(
          NonEmptyChain.one(
            new Exception(
              s"Type differ on $id [answer ${answer.name}] != [field ${fieldWithMeta.field.name}]"
            )
          )
        )

    case _ => Left(NonEmptyChain.one(new Exception("idk")))
  }

}
