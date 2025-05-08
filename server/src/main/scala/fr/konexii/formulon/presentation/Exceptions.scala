package fr.konexii.formulon.presentation

import cats._
import cats.syntax.all._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._

object Exceptions
    extends ValidationExceptionInstances
    with ValidatorExceptionInstances
    with KeyedExceptionWithMessagesInstances

trait ValidationExceptionInstances {

  implicit val showForValidationException: Show[InvariantsException] =
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

  // TODO : find a way to seal all of that
  implicit def showForValidatorException[E: Show]: Show[ValidatorException[E]] =
    new Show[ValidatorException[E]] {
      def show(t: ValidatorException[E]): String = t match {
        case RequiredFieldNotFound(id) =>
          s"Field with id $id is required in the blueprint but was not found in the answers."
        case PluginException(e) =>
          s"An exception was raised by a plugin: ${e.show}"
        case PluginNotFound(id, pluginName) =>
          s"Plugin with name ${pluginName} could not be found at id $id."
        case _: ValidatorException[_] => "An unknown exception was thrown."
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
