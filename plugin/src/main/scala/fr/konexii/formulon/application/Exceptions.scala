package fr.konexii.formulon.application

import fr.konexii.formulon.domain._

/*
 * Since we cannot import show instances exceptions hold a key and a message.
 * The key for i18n and the message for logging the exception.
 */
trait KeyedExceptionWithMessage extends KeyedException {

  def message: String

}

object KeyedExceptionWithMessage {

  def fromKeyedException(
      ke: KeyedException,
      msg: String
  ): KeyedExceptionWithMessage = new KeyedExceptionWithMessage {
    override def key = ke.key
    def message = msg
  }

}
