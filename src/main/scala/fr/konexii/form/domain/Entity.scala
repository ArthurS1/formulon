package fr.konexii.form
package domain

import java.util.UUID

import cats.Functor
import cats.effect.kernel.Sync
import cats.effect.std.UUIDGen

final case class Entity[T](id: UUID, data: T)

object Entity extends EntityInstances {

  def apply[T, F[_]](
      data: T
  )(implicit F: UUIDGen[F], G: Functor[F]): F[Entity[T]] =
    G.map(F.randomUUID)(Entity(_, data))

}

sealed abstract private[domain] class EntityInstances {

  implicit val functorForEntity: Functor[Entity] = new Functor[Entity] {
    def map[A, B](fa: Entity[A])(f: A => B): Entity[B] =
      Entity(fa.id, f(fa.data))
  }

}
