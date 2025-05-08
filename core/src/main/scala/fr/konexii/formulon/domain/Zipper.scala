package fr.konexii.formulon.domain

import cats.syntax.all._

import fr.konexii.formulon.domain.Tree._

import java.util.UUID

sealed trait ZipperException extends KeyedException {
  def history: List[ZipperHistory]
}
sealed case class FailedToFind(id: UUID, history: List[ZipperHistory])
    extends ZipperException
sealed case class FoundInTwoBranches(id: UUID, history: List[ZipperHistory])
    extends ZipperException
sealed case class FailedToMoveOut(
    id: Option[UUID],
    history: List[ZipperHistory]
) extends ZipperException
sealed case class FailedToMoveNext(history: List[ZipperHistory])
    extends ZipperException
sealed case class FailedToReplayHistory(history: List[ZipperHistory])
    extends ZipperException
sealed case class FailedToReplayHistoryBeforeRoot(history: List[ZipperHistory])
    extends ZipperException

sealed trait ZipperHistory {
  def nodeId: UUID
}
sealed case class HNext(nodeId: UUID) extends ZipperHistory
sealed case class HOut(nodeId: UUID) extends ZipperHistory
sealed case class HRoot(nodeId: UUID) extends ZipperHistory

final case class Zipper[T](
    focus: Tree[Entity[T]],
    history: List[ZipperHistory],
    original: Tree[Entity[T]]
) {

  type Result = Either[ZipperException, Zipper[T]]

  def find(uuid: UUID): Result = {
    lazy val outFind = this.out.flatMap(_.find(uuid))
    lazy val nextFind = this.next.flatMap(_.find(uuid))

    this.focus.id match {
      case None                          => Left(FailedToFind(uuid, history))
      case Some(value) if value === uuid => Right(this)
      case Some(value) =>
        (nextFind, outFind) match {
          case (Right(next), Left(_)) => Right(next)
          case (Left(_), Right(find)) => Right(find)
          case (Right(_), Right(_)) => Left(FoundInTwoBranches(value, history))
          case (Left(error), Left(_)) => Left(error)
        }
    }
  }

  def next: Result =
    focus match {
      case Trunk(Entity(id, _), next) =>
        Right(Zipper(next, HNext(id) :: history, original))
      case Branch(Entity(id, _), next, _) =>
        Right(Zipper(next, HNext(id) :: history, original))
      case _ =>
        Left(FailedToMoveNext(history))
    }

  def out: Result =
    focus match {
      case Branch(Entity(id, _), _, out) =>
        Right(Zipper(out, HOut(id) :: history, original))
      case _ =>
        Left(FailedToMoveOut(focus.id, history))
    }

  def previous: Result =
    if (history.isEmpty)
      Left(FailedToReplayHistoryBeforeRoot(history))
    else
      replayHistory(
        history.tail,
        List(),
        original
      )

  def content: Option[Entity[T]] =
    this.focus match {
      case Branch(content, next, out) => Some(content)
      case Trunk(content, next) => Some(content)
      case End() => None
    }

  private def replayHistory(
      h: List[ZipperHistory],
      hAcc: List[ZipperHistory],
      current: Tree[Entity[T]]
  ): Result =
    (h, current) match {
      case (Nil, _) =>
        Right(Zipper(current, hAcc, original))
      case ((v: HNext) :: rest, Trunk(_, next)) =>
        replayHistory(rest, v :: hAcc, next)
      case ((v: HNext) :: rest, Branch(_, next, _)) =>
        replayHistory(rest, v :: hAcc, next)
      case ((v: HOut) :: rest, Branch(_, _, out)) =>
        replayHistory(rest, v :: hAcc, out)
      case (h, _) =>
        Left(FailedToReplayHistory(h))
    }

}

object Zipper {

  def apply[T](st: Tree[Entity[T]]): Zipper[T] =
    new Zipper(st, List(), st)

}
