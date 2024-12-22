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
import fr.konexii.form.application.dtos.SchemaRequest
import fr.konexii.form.domain.Entity

class Routes(repositories: Repositories[IO]) {

  val infrastructureRoutes = HttpRoutes
    .of[IO] { case GET -> Root / "ping" =>
      Ok("pong")
    }

  val contentRoutes = HttpRoutes
    .of[IO] { case req @ POST -> Root / "schema" / id / "content" =>
      Ok("")
    }

  val schemaRoutes = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "schema" =>
        for {
          newSchema <- req.as[SchemaRequest]
          createdSchema <- new usecases.CreateSchema(repositories)
            .execute(newSchema)
          response <- Ok("")
        } yield response
      case GET -> Root / "schema" / id =>
        for {
          schema <- new usecases.ReadSchema[IO](repositories).execute(id)
          response <- Ok("")
        } yield response
      case req @ PUT -> Root / "schema" / id =>
        for {
          update <- req.as[Entity[SchemaRequest]]
          updatedSchema <- new usecases.UpdateSchema[IO](repositories)
            .execute(update)
          response <- Ok("")
        } yield response
      case DELETE -> Root / "schema" / id =>
        new usecases.DeleteSchema[IO](repositories).execute(id) >> Ok()
    }

  val routes = schemaRoutes <+> contentRoutes <+> infrastructureRoutes

}
