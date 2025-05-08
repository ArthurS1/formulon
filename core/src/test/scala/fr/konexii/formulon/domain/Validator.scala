package fr.konexii.formulon.domain

import cats.data._

import fr.konexii.formulon.domain.Validator._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues._

import java.util.UUID

final case class TestException()

final case class TestField(name: String = "test") extends Field

final case class TestAnswer(name: String = "test") extends Answer

class ValidatorSuite extends AnyFunSpec {

  def alwaysValidateNext(z: Zipper[Validator.Association]): Either[
    NonEmptyChain[ValidatorException[TestException]],
    Zipper[Validator.Association]
  ] = z.next.left.map(_ => NonEmptyChain.one(PluginException(TestException())))

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

    describe("when checking for required fields") {

      it("is valid when the required answer is present") {
        assert(
          failsRequirementCheck(
            (Some(TestAnswer()), fieldWithMetadata)
          ) === false
        )
      }

      it("is valid when the answer is absent but not required") {
        assert(
          failsRequirementCheck(
            (None, fieldWithMetadata.copy(required = false))
          ) === false
        )
      }

      it("is invalid when the required answer is absent") {
        assert(
          failsRequirementCheck((None, fieldWithMetadata))
        )
      }

    }

    describe("when validating answers against a blueprint") {

      it("should work under normal conditions (minimal)") {
        val z: Zipper[Association] =
          Zipper(Trunk(Entity(id, (Option(answer), fieldWithMetadata)), End()))
        assertResult(End())(validateSingle(z, alwaysValidateNext).value.focus)
      }

      it(
        "should fail if a field is required and no answer is found (minimal)"
      ) {
        val z: Zipper[Association] =
          Zipper(Trunk(Entity(id, (None, fieldWithMetadata)), End()))
        assertResult(Left(NonEmptyChain.one(RequiredFieldNotFound(id))))(
          validateSingle(z, alwaysValidateNext)
        )
      }

      it(
        "should fail if the type of field and answer differs"
      ) {
        val z: Zipper[Association] =
          Zipper(
            Trunk(
              Entity(
                id,
                (
                  Some(TestAnswer().copy(name = "a")),
                  fieldWithMetadata.copy(field = TestField().copy(name = "b"))
                )
              ),
              End()
            )
          )
        assertResult(Left(NonEmptyChain.one(TypesDiffer(id, "a", "b"))))(
          validateSingle(z, alwaysValidateNext)
        )
      }

    }

  }

}
