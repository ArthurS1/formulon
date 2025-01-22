package fr.konexii.form
package application
package dtos

import fr.konexii.form.domain.Schema

case class CreateSchemaRequest(name: String)

object CreateSchemaRequest {

  object implicits {
    implicit def schemaRequestToSchema(schemaRequest: CreateSchemaRequest): Schema =
      Schema(
        name = schemaRequest.name,
        versions = List(),
        active = None
      )
  }

}
