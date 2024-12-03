package fr.konexii.form
package application
package usecases

import cats.implicits._
import cats._

import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.Schema
import fr.konexii.form.domain.Entity

class ReadSchema[F[_]](repositories: Repositories[F])(implicit F: MonadThrow[F]) {

  def execute(id: String): F[Entity[Schema]] = for {
    schemaOption <- repositories.schema.get(id)
    schema <- F.fromOption(schemaOption, new Exception("schema not found"))
  } yield schema

}
