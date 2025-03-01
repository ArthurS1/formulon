package fr.konexii.form.domain

import cats._
import cats.effect._
import cats.syntax.all._

import java.time._

import fr.konexii.form.domain.field._

final case class SchemaVersion(
    date: LocalDateTime,
    content: SchemaTree[Entity[FieldWithMetadata]]
)

object SchemaVersion {

  def apply[F[_]](
      content: SchemaTree[Entity[FieldWithMetadata]]
  )(implicit F: Sync[F]): F[SchemaVersion] =
    for {
      fd <- F.realTime
      ldt <- localDateTimeFromMilis(fd.toMillis)
    } yield SchemaVersion(ldt, content)

  private def localDateTimeFromMilis[F[_]](
      milis: Long
  )(implicit F: ApplicativeThrow[F]): F[LocalDateTime] =
    F.catchOnly[DateTimeException](
      LocalDateTime.ofInstant(Instant.ofEpochMilli(milis), ZoneOffset.UTC)
    )

}
