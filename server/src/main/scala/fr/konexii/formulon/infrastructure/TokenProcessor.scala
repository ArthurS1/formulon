package fr.konexii.formulon.infrastructure

import fr.konexii.formulon.application.Role

trait TokenProcessor {

  type ErrorMessage = String

  def decodeAndValidate(
      token: String,
      key: String
  ): Either[ErrorMessage, Role]

}
