package fr.konexii.formulon.domain

import cats._
import cats.effect.std.UUIDGen

import java.util.UUID

final case class Entity[+T](id: UUID, data: T)

object Entity extends EntityInstances {

  def generateUUID[T, F[_]](
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
