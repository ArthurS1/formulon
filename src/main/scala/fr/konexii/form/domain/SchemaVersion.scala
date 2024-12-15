package fr.konexii.form
package domain

import cats.effect.kernel.Clock
import cats.Functor
import cats.syntax.functor._
import java.time.LocalDateTime
import scala.concurrent.duration.FiniteDuration

import fr.konexii.form.domain.Block
import java.time.Instant
import java.time.ZoneId

final case class SchemaVersion(
    date: LocalDateTime,
    content: Entity[Block]
)

object SchemaVersion {

  def apply[F[_]](
      content: Entity[Block]
  )(implicit G: Clock[F], F: Functor[F]): F[SchemaVersion] =
    F.map(G.realTime)((fd: FiniteDuration) =>
      SchemaVersion(
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(fd.toMillis), ZoneId.systemDefault()),
        content
      )
    )

}
