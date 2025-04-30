package fr.konexii.formulon.domain

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues._

import java.util.UUID

class ZipperSuite extends AnyFunSpec {

  describe("A tree of entities") {

    describe("when moving") {

      describe("to the next node") {

        it("should work if it is defined") {
          val id = UUID.randomUUID()
          val z: Zipper[String] =
            Zipper(Trunk(Entity(id, "a"), End()))

          assertResult(End())(z.next.value.focus)
          assertResult(List(HNext(id)))(z.next.value.history)
        }

        it("should fail if it is undefined") {
          val z: Zipper[String] = Zipper(End())

          assertResult(z.next)(Left(FailedToMoveNext(List())))
        }

      }

      describe("to the out node") {

        it("should fail if the current is a trunk") {
          val id = UUID.randomUUID()
          val z: Zipper[String] = Zipper(Trunk(Entity(id, "a"), End()))

          assertResult(Left(FailedToMoveOut(Some(id), List())))(z.out)
        }

        it("should fail if the current is an end") {
          val z: Zipper[String] = Zipper(End())

          assertResult(Left(FailedToMoveOut(None, List())))(z.out)
        }

        it("should succeed if the current is a branch") {
          val id = UUID.randomUUID()
          val z: Zipper[String] =
            Zipper(
              Branch(
                Entity(id, "a"),
                End(),
                Trunk(Entity(id, "a"), End())
              )
            )

          assertResult(Trunk(Entity(id, "a"), End()))(z.out.value.focus)
        }

      }

      describe("to a previous node") {

        it("should fail when at root") {
          val z: Zipper[String] = Zipper(End())

          assertResult(Left(FailedToReplayHistoryBeforeRoot(Nil)))(z.previous)
        }

        it("should work on a minimal tree") {
          val id = UUID.randomUUID()
          val z: Zipper[String] = Zipper(Trunk(Entity(id, "a"), End()))

          assertResult(List(HNext(id)), "history check before")(
            z.next.value.history
          )
          assertResult(End(), "focus check before")(
            z.next.value.focus
          )
          assertResult(List(), "history check after")(
            z.next.value.previous.value.history
          )
          assertResult(z.focus, "focus check after")(
            z.next.value.previous.value.focus
          )
        }

        it("should work on a tree with two nodes") {
          val idA = UUID.randomUUID()
          val idB = UUID.randomUUID()
          val z: Zipper[String] =
            Zipper(
              Trunk(Entity(idA, "a"), Branch(Entity(idB, "b"), End(), End()))
            )

          assertResult(List(HNext(idB), HNext(idA)), "history check before")(
            z.next.value.next.value.history
          )
          assertResult(End(), "focus check before")(
            z.next.value.next.value.focus
          )
          assertResult(List(), "history check after previous(2)")(
            z.next.value.next.value.previous.value.previous.value.history
          )
          assertResult(z.focus, "focus check after previous(2)")(
            z.next.value.next.value.previous.value.previous.value.focus
          )
        }

      }

    }

    describe("when finding a specific node") {

      it("finds a node that exists") {
        val idA = UUID.randomUUID()
        val idB = UUID.randomUUID()
        val z: Zipper[String] =
          Zipper(
            Trunk(Entity(idA, "a"), Branch(Entity(idB, "b"), End(), End()))
          )

        assertResult(z.find(idB).value)(z.next.value)
      }

      it("fails to find a node that does not exist") {
        val idA = UUID.randomUUID()
        val idB = UUID.randomUUID()
        val idC = UUID.randomUUID()
        val z: Zipper[String] =
          Zipper(
            Trunk(Entity(idA, "a"), Branch(Entity(idB, "b"), End(), End()))
          )

        assertResult(z.find(idC))(
          Left(FailedToFind(idC, List(HNext(idB), HNext(idA))))
        )
      }

    }

  }

}
