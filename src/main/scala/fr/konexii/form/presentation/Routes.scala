package fr.konexii.form
package presentation

/*
 * TODO : There should be a way to remove dependency on cats effect's IO
 */

import cats.effect._
import cats._
import cats.syntax.all._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

import java.util.UUID

import fr.konexii.form.application._
import fr.konexii.form.domain._
import fr.konexii.form.application.dtos._

class Routes(repositories: Repositories[IO]) {

  val infrastructureRoutes = HttpRoutes
    .of[IO] { case GET -> Root / "ping" =>
      Ok("pong")
    }

  val versionRoutes = HttpRoutes
    .of[IO] {
      // add a new version to the schema
      case req @ POST -> Root / "schema" / id / "version" / "add" =>
        /*for {
          request <- req.as[CreateVersionRequest]
          newVersion <-
          response <- Ok()
        } yield response*/
        Ok("")
      // return all available versions
      case req @ GET -> Root / "schema" / id / "version" =>
        Ok("")
      // return a specific version with its content
      case req @ GET -> Root / "schema" / id / "version" / versionId =>
        Ok("")
      // return active version schema
      case req @ GET -> Root / "schema" / id / "version" / "active" =>
        Ok("")
      // update active version to the id
      case req @ PUT -> Root / "schema" / id / "version" / "active" / versionId =>
        Ok("")
      // remove active version (shutdown the schema)
      case req @ DELETE -> Root / "schema" / id / "version" / "active" =>
        Ok("")
    }

  val schemaRoutes = HttpRoutes
    .of[IO] {
      // create a schema
      case req @ POST -> Root / "schema" =>
        for {
          newSchema <- req.as[CreateSchemaRequest]
          createdSchema <- new usecases.CreateSchema(repositories)
            .execute(newSchema)
          response <- Ok(createdSchema)
        } yield response
      // get the schema with the id and its active version content
      case GET -> Root / "schema" / id =>
        for {
          schema <- new usecases.ReadSchema[IO](repositories).execute(id)
          response <- Ok(schema)
        } yield response
      // update the schema
      case req @ PUT -> Root / "schema" / id =>
        for {
          update <- req.as[UpdateSchemaRequest]
          updatedSchema <- new usecases.UpdateSchema[IO](repositories)
            .execute(update, id)
          response <- Ok(updatedSchema)
        } yield response
      // delete the schema
      case DELETE -> Root / "schema" / id =>
        new usecases.DeleteSchema[IO](repositories).execute(id) >> Ok()
    }

  val routes = schemaRoutes <+> versionRoutes <+> infrastructureRoutes

}
