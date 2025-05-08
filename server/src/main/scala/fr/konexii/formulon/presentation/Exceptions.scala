package fr.konexii.formulon.presentation

import cats._
import cats.syntax.all._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application
import fr.konexii.formulon.domain

object Exceptions
    extends InvariantsExceptionInstances
    with ValidatorExceptionInstances
    with ValidationExceptionInstances
    with KeyedExceptionWithMessagesInstances

trait InvariantsExceptionInstances {

  implicit val showForInvariantException: Show[InvariantsException] =
    new Show[InvariantsException] {

      def show(t: InvariantsException): String =
        s"String validation : ${t match {
            case Empty()             => "illegal blank string."
            case NotAlphaNumeric()   => "should be alphanumeric only."
            case NotUnicodeLetters() => "should be unicode letters only."
            case TooLong(nbMaxChar) =>
              s"too long string (max $nbMaxChar characters)."
          }}"

    }

}

trait ValidatorExceptionInstances {

  implicit def showForValidatorException[E: Show]: Show[ValidatorException[E]] =
    new Show[ValidatorException[E]] {
      def show(t: ValidatorException[E]): String = t match {
        case domain.PluginException(e) =>
          s"An exception was raised by a plugin : ${e.show}"
        case RequiredFieldNotFound(id) =>
          s"Field with id $id is required in the blueprint but was not found in the answers."
        case TypesDiffer(id, typeA, typeB) =>
          s"Types differ at id $id : $typeA paired wrongly with $typeB."
      }
    }

}

trait KeyedExceptionWithMessagesInstances {

  implicit def showForKeyedExceptionsWithMessages
      : Show[KeyedExceptionWithMessage] =
    new Show[KeyedExceptionWithMessage] {
      def show(t: KeyedExceptionWithMessage): String =
        s"${t.message} [${t.key}]"
    }

}

trait ValidationExceptionInstances {

  implicit def showForValidationException(implicit
      a: Show[KeyedExceptionWithMessage]
  ): Show[ValidationException] =
    new Show[ValidationException] {
      def show(t: ValidationException): String = t match {
        case PluginNotFound(id, pluginName) =>
          s"Plugin with name ${pluginName} could not be found at id $id."
        case FailedToGetZipperContent() =>
          s"Could not get zipper content. This is caused by the current node being an End()."
        case application.PluginException(e) =>
          s"An exception was raised by a plugin: ${e.show}."
      }
    }

}
