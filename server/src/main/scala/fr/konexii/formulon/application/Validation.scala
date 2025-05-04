package fr.konexii.formulon.application

import cats.data._
import cats.syntax.all._

import java.util.UUID

import fr.konexii.formulon.domain._

final case class PluginNotFound(id: UUID, pluginName: String)
    extends ValidatorException
final case class FailedToGetZipperContent() extends ValidatorException

object Validation {

  def validateWrapper(plugins: List[Plugin]): Validator.Validation =
    (z: Zipper[Validator.Association]) => {
      val plugin = for {
        association <- Either.fromOption(z.content, FailedToGetZipperContent())
        plugin <- Either.fromOption(
          plugins.find(p => p.name === association.data._2.field.name),
          PluginNotFound(association.id, association.data._2.field.name)
        )
      } yield plugin

      plugin.left
        .map(NonEmptyChain.one(_))
        .flatMap(_.validate(z))
    }

}
