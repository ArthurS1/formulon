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

  // Ugly, could be better
  private def validate(plugins: List[Plugin])(
      z: Zipper[Validator.Association]
  ): Either[
    NonEmptyChain[Throwable],
    Zipper[Validator.Association]
  ] = z.focus match {
    case Branch(Entity(id, (Some(answer), fieldWithMetadata)), _, _) =>
      if (answer.name === fieldWithMetadata.field.name)
        Either
          .fromOption(
            plugins.find(p => p.name === fieldWithMetadata.field.name),
            NonEmptyChain.one(new Exception("failed to find"))
          )
          .flatMap(plugin => plugin.validate(z))
      else
        Left(NonEmptyChain.one(new Exception("answer and field types differ")))
    case Trunk(Entity(id, (Some(answer), fieldWithMetadata)), _) =>
      if (answer.name === fieldWithMetadata.field.name)
        Either
          .fromOption(
            plugins.find(p => p.name === fieldWithMetadata.field.name),
            NonEmptyChain.one(new Exception("failed to find"))
          )
          .flatMap(plugin => plugin.validate(z))
      else
        Left(NonEmptyChain.one(new Exception("answer and field types differ")))
    case _                =>
        Left(NonEmptyChain.one(new Exception("idk")))
  }

}
