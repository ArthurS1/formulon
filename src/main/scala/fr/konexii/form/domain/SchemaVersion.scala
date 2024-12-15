package fr.konexii.form
package domain

import cats.ApplicativeThrow
import cats.effect.kernel.Sync
import cats.syntax.all._
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import scala.concurrent.duration.FiniteDuration

import fr.konexii.form.domain.Block

final case class SchemaVersion(
    date: LocalDateTime,
    content: Entity[Block]
)

object SchemaVersion {

  def apply[F[_]](
      content: Entity[Block]
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
