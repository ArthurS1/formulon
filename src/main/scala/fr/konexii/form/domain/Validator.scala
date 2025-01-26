package fr.konexii.form
package domain

import cats._
import cats.data._

import response.Response

object Validator {

  /*
   * This is supposed to validate the given response against the given schema
   */
  def validate(
      schema: SchemaTree[FieldWithMetadata],
      response: Entity[Response]
  ): ValidatedNec[String, Response] = ???

  /* TODO:
     find in the schema the corresponding field
     validate against the given schema type
     with private functions
   */

}
