package fr.konexii.form
package domain

import cats.ApplicativeThrow
import cats.effect.kernel.Clock
import cats.MonadThrow
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

  /* Cannot do better for now, see https://discord.com/channels/632277896739946517/632278585700384799/1317833443564064802*/

  def apply[F[_]](
      content: Entity[Block]
  )(implicit G: Clock[F], F: MonadThrow[F]): F[SchemaVersion] = {
    val localDateTime: F[LocalDateTime] =
      F.flatMap(G.realTime)((fd: FiniteDuration) =>
        localDateTimeFromMilis(fd.toMillis)
      )
    F.map(localDateTime)(SchemaVersion(_, content))
  }

  private def localDateTimeFromMilis[F[_]](
      milis: Long
  )(implicit F: MonadThrow[F]): F[LocalDateTime] =
    F.catchOnly[DateTimeException](
      LocalDateTime.ofInstant(Instant.ofEpochMilli(milis), ZoneOffset.UTC)
    )

}
