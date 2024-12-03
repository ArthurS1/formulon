package fr.konexii.form
package presentation

/*
 * TODO : There should be a way to remove dependency on cats effect's IO
 */

import cats.effect._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

import java.util.UUID

import fr.konexii.form.application._

class Routes(repositories: Repositories[IO]) {

  val routes = HttpRoutes
    .of[IO] {
      case req @ POST -> Root / "schema" =>
        for {
          newSchema <- req.as[domain.Schema]
          createdSchema <- new usecases.CreateSchema(repositories)
            .execute(newSchema)
          response <- Ok(createdSchema.asJson)
        } yield response
      case GET -> Root / "schema" / id =>
        for {
          schema <- new usecases.ReadSchema[IO](repositories).execute(id)
          response <- Ok(schema.asJson)
        } yield response
      case req @ PUT -> Root / "schema" / id =>
        for {
          update <- req.as[domain.Entity[domain.Schema]]
          updatedSchema <- new usecases.UpdateSchema[IO](repositories).execute(update)
          response <- Ok(updatedSchema.asJson)
        } yield response
      case DELETE -> Root / "schema" / id =>
        new usecases.DeleteSchema[IO](repositories).execute(id) >> Ok()
      case GET -> Root / "ping" => Ok("pong")
    }

}
