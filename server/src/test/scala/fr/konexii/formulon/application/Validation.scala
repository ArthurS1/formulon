package fr.konexii.formulon.application

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues._

import cats.data._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.Validation.validateWrapper
import java.util.UUID
import io.circe.Json

final case class TestException() extends ValidatorException

final case class TestField(name: String = "test") extends Field

final case class TestAnswer(name: String = "test") extends Answer

class PluginSpy(
  val name: String,
  var validateTimesCalled: Int = 0
) extends Plugin {

  def validate: Validator.Validation = z => {
    validateTimesCalled += 1
    Right(z)
  }

  def serializeField(field: Field): Either[ValidatorException, Json] = ???

  def deserializeField(field: Json): Either[ValidatorException, Field] = ???

  def serializeAnswer(answer: Answer): Either[ValidatorException, Json] = ???

  def deserializeAnswer(answer: Json): Either[ValidatorException, Answer] = ???

}

class ValidationSuite extends AnyFunSpec {

  describe("Validation with plugins integration") {

    describe("when the required plugin does not exist") {

      it("should fail and say which plugin fails") {
        val plugins = List()
        val id = UUID.randomUUID
        val fwm = FieldWithMetadata("test", true, TestField()).toEither.value
        val answer: Answer = TestAnswer()
        val z = Zipper(Trunk(Entity(id, (Option(answer), fwm)), End()))
        val a = validateWrapper(plugins)(z)

        assertResult(Left(Chain(PluginNotFound(id, "test"))))(a)
      }

    }

    describe("when the zipper node is End") {

      it("should fail") {
        val plugins = List()
        val z = Zipper(End[Entity[Validator.Association]]())
        val a = validateWrapper(plugins)(z)

        assertResult(Left(Chain(FailedToGetZipperContent())))(a)
      }

    }

    describe("when in valid context") {

      it("should call the validate function of the right plugin") {
        val pluginA = new PluginSpy(name = "a")
        val pluginB = new PluginSpy(name = "b")
        val plugins = List(pluginA, pluginB)
        val id = UUID.randomUUID
        val fwm = FieldWithMetadata("test", true, TestField().copy("a")).toEither.value
        val answer: Answer = TestAnswer().copy(name = "b")
        val z = Zipper(Trunk(Entity(id, (Option(answer), fwm)), End()))
        val a = validateWrapper(plugins)(z)

        assert(pluginA.validateTimesCalled === 1)
        assert(pluginB.validateTimesCalled === 0)
        assertResult(Right(z))(a)
      }

    }

  }

}
