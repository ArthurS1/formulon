package fr.konexii.formulon.domain

import cats.data._

import fr.konexii.formulon.domain.Validator._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues._
import java.util.UUID

final case class TestException() extends ValidatorException

final case class TestField() extends Field {
  val name = "test"
}

final case class TestAnswer() extends Answer {
  val name = "test"
}

class ValidatorSuite extends AnyFunSpec {

  def alwaysValidateNext(z: Zipper[Validator.Association]): Either[
    NonEmptyChain[ValidatorException],
    Zipper[Validator.Association]
  ] = z.next.left.map(_ => NonEmptyChain.one(TestException()))

  describe("Validator service") {
    val id = UUID.randomUUID()
    val fieldWithMetadata =
      FieldWithMetadata("test", true, TestField()).toEither.value
    val answer = TestAnswer()

    describe("when associating a submission with a blueprint") {

      it("should work with test field and test answers") {
        assertResult(
          Trunk(Entity(id, (Some(answer), fieldWithMetadata)), End())
        )(
          association(
            Trunk(Entity(id, fieldWithMetadata), End()),
            Submission(List(Entity(id, answer)))
          )
        )
      }

      it("should work with test field but no answer") {
        assertResult(
          Trunk(Entity(id, (None, fieldWithMetadata)), End())
        )(
          association(
            Trunk(Entity(id, fieldWithMetadata), End()),
            Submission(List())
          )
        )
      }

    }

    describe("when checking for field requirement") {

      it("is valid when the required answer is present") {
        assert(
          checkRequired(
            Entity(id, (Some(TestAnswer()), fieldWithMetadata))
          )
        )
      }

      it("is invalid when the required answer is absent") {
        assert(
          checkRequired(Entity(id, (None, fieldWithMetadata))) === false
        )
      }

      it("is valid when the answer is absent but not required") {
        assert(
          checkRequired(
            Entity(id, (None, fieldWithMetadata.copy(required = false)))
          )
        )
      }

    }

    describe("when validating answers against a blueprint") {

      it("should work under normal conditions (minimal)") {
        val z: Zipper[Association] =
          Zipper(Trunk(Entity(id, (Option(answer), fieldWithMetadata)), End()))
        assertResult(End())(validateSingle(z, alwaysValidateNext).value.focus)
      }

      it("should fail if a field is required and no answer is found (minimal)") {
        val z: Zipper[Association] =
          Zipper(Trunk(Entity(id, (None, fieldWithMetadata)), End()))
        assertResult(Left(NonEmptyChain.one(RequiredFieldNotFound(id))))(
          validateSingle(z, alwaysValidateNext)
        )
      }

    }

  }

}
