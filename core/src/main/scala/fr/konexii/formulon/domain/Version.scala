package fr.konexii.formulon.domain

import cats._
import cats.effect._
import cats.syntax.all._

import java.time._

final case class Version(
    date: LocalDateTime,
    content: Tree[Entity[FieldWithMetadata]]
)

object Version {

  def apply[F[_]](
      content: Tree[Entity[FieldWithMetadata]]
  )(implicit F: Sync[F]): F[Version] =
    for {
      fd <- F.realTime
      ldt <- localDateTimeFromMilis(fd.toMillis)
    } yield Version(ldt, content)

  private def localDateTimeFromMilis[F[_]](
      milis: Long
  )(implicit F: ApplicativeThrow[F]): F[LocalDateTime] =
    F.catchOnly[DateTimeException](
      LocalDateTime.ofInstant(Instant.ofEpochMilli(milis), ZoneOffset.UTC)
    )

}
