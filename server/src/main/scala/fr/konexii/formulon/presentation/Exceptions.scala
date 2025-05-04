package fr.konexii.formulon.presentation

import fr.konexii.formulon.domain._

import cats.Show

object ValidationExceptionInstances {

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

object ValidatorExceptionInstances {

  implicit def showForValidatorException: Show[ValidatorException] =
    new Show[ValidatorException] {
      def show(t: ValidatorException): String = t match {
        case RequiredFieldNotFound(id) =>
          s"Field with id $id is required in the blueprint but was not found in the answers."
        case _: ValidatorException => "An unknown exception was thrown."
      }

    }

}
