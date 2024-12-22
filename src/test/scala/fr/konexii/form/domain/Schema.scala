package fr.konexii.form
package domain

import org.scalatest.funsuite.AnyFunSuite

import fr.konexii.form.domain.Schema
import java.time.LocalDateTime
import java.util.UUID

class SchemaSuite extends AnyFunSuite {
  test("Create a schema") {
    Schema(name = "hello")
  }

  test("Add a version to a schema") {
    val s = Schema(name = "hello")
    val v = Entity(
      UUID.randomUUID(),
      SchemaVersion(
        date = LocalDateTime.now(),
        content = Entity(UUID.randomUUID(), End())
      )
    )

    val s1 = s.addNewVersion(v)

  }

  test("Set active version for the schema") {


  }

  test("Unset active version for the schema") {

  }
}
