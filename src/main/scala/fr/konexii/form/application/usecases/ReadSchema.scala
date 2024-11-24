package fr.konexii.form.usecases

import fr.konexii.form.application.Repositories
import fr.konexii.form.domain.Schema

import cats.implicits._
import cats._

class ReadSchema[F[_]](repositories: Repositories[F])(implicit F: MonadThrow[F]) {

  def execute(id: String): F[Schema] = for {
    schemaOption <- repositories.schema.get(id)
    schema <- F.fromOption(schemaOption, new Exception("schema not found"))
  } yield schema

}
