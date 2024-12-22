package fr.konexii.form
package application
package dtos

import fr.konexii.form.domain.Schema

case class SchemaRequest(name: String)

object SchemaRequest {

  object implicits {
    implicit def schemaRequestToSchema(schemaRequest: SchemaRequest): Schema =
      Schema(
        name = schemaRequest.name,
        versions = List(),
        active = None
      )
  }

}
