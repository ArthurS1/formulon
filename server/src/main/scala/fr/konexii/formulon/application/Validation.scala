package fr.konexii.formulon.application

import cats.data._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._

sealed trait ValidationException extends KeyedException
sealed case class PluginNotFound(id: UUID, pluginName: String)
    extends ValidationException
sealed case class FailedToGetZipperContent()
    extends ValidationException
sealed case class PluginException(ke: KeyedExceptionWithMessage)
    extends ValidationException

sealed trait T
sealed case class ValidatorException_(e: ValidatorException[KeyedExceptionWithMessage]) extends T
sealed case class ValidationException_(e: ValidationException) extends T

object Validation {

  def validateWrapper(
      plugins: List[Plugin]
  ): Validator.Validation[ValidationException] =
    (z: Zipper[Validator.Association]) => {
      val plugin = for {
        association <- Either.fromOption(z.content, FailedToGetZipperContent())
        plugin <- Either.fromOption(
          plugins.find(p => p.name === association.data._2.field.name),
          PluginNotFound(association.id, association.data._2.field.name)
        )
      } yield plugin

      plugin
        .left
        .map(NonEmptyChain.one(_))
        .flatMap(_.validate(z).left.map(e => e.map(ke => PluginException(ke))))
    }

}
