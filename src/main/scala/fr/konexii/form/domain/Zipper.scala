package fr.konexii.form
package domain

import cats._
import cats.effect._
import cats.syntax.all._

import java.util.UUID

final case class ZipperError(history: List[ZipperHistory], msg: String)
    extends Throwable

sealed trait ZipperHistory {
  def nodeId: UUID
}

sealed case class HNext(nodeId: UUID) extends ZipperHistory
sealed case class HLeft(nodeId: UUID) extends ZipperHistory
sealed case class HRight(nodeId: UUID) extends ZipperHistory
sealed case class HRoot(nodeId: UUID) extends ZipperHistory

final case class Zipper[T, F[_]: Async](
    focus: SchemaTree[T],
    history: List[ZipperHistory],
    private val original: SchemaTree[T]
) {

  def find(uuid: UUID): F[Zipper[T, F]] = {
    lazy val leftFind = this.left.flatMap(_.find(uuid))
    lazy val rightFind = this.right.flatMap(_.find(uuid))
    lazy val nextFind = this.next.flatMap(_.find(uuid))

    if (this.id === uuid)
      MonadThrow[F].pure(this)
    else
      Async[F]
        .race(nextFind, Async[F].race(leftFind, rightFind))
        .map(
          _ match {
            case Left(value)         => value
            case Right(Left(value))  => value
            case Right(Right(value)) => value
          }
        )
        .handleErrorWith(f =>
          f match {
            case err @ (_: ZipperError) => ApplicativeThrow[F].raiseError(err)
            case _ =>
              ApplicativeThrow[F]
                .raiseError(new ZipperError(history, s"Failed to find id $id"))
          }
        )
  }

  def id = this.focus match {
    case Block(Entity(id, _), next)       => id
    case Branch(condition, Entity(id, _)) => id
  }

  def next: F[Zipper[T, F]] =
    focus match {
      case Block(Entity(id, _), Some(next)) =>
        MonadThrow[F].pure(Zipper(next, HNext(id) :: history, original))
      case _ =>
        MonadThrow[F].raiseError(
          new ZipperError(history, "Failed to move to next block")
        )
    }

  def left: F[Zipper[T, F]] =
    focus match {
      case Branch(_, Entity(id, (Some(next), _))) =>
        MonadThrow[F].pure(Zipper(next, HLeft(id) :: history, original))
      case _ =>
        MonadThrow[F].raiseError(new ZipperError(history, "Failed to go left."))
    }

  def right: F[Zipper[T, F]] =
    focus match {
      case Branch(_, Entity(id, (_, Some(next)))) =>
        MonadThrow[F].pure(Zipper(next, HRight(id) :: history, original))
      case _ =>
        MonadThrow[F].raiseError(
          new ZipperError(history, "Failed to go right.")
        )
    }

  def previous: F[Zipper[T, F]] =
    replayHistory(history, original, original)

  private def replayHistory(
      h: List[ZipperHistory],
      current: SchemaTree[T],
      original: SchemaTree[T]
  ): F[Zipper[T, F]] =
    (h, current) match {
      case (ctx @ (_ :: Nil), _) =>
        MonadThrow[F].pure(Zipper(current, ctx, original))
      case ((_: HNext) :: rest, Block(_, Some(next))) =>
        replayHistory(rest, next, original)
      case ((_: HLeft) :: rest, Branch(_, Entity(_, (Some(next), _)))) =>
        replayHistory(rest, next, original)
      case ((_: HRight) :: rest, Branch(_, Entity(_, (_, Some(next))))) =>
        replayHistory(rest, next, original)
      case (h, _) =>
        MonadThrow[F].raiseError(
          new ZipperError(h, "Failed to replay history.")
        )
    }
}

object Zipper {

  def apply[T, F[_]: Async](st: SchemaTree[T]): Zipper[T, F] =
    new Zipper(st, List(), st)

}
