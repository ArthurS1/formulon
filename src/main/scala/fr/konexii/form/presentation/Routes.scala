package fr.konexii.form.presentation

import cats._
import cats.effect._
import cats.syntax.all._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

import java.util.UUID

import fr.konexii.form.domain._
import fr.konexii.form.application._
import fr.konexii.form.application.dtos._
import fr.konexii.form.presentation.Serialization._

class Routes(repositories: Repositories[IO]) {

  val infrastructureRoutes = HttpRoutes
    .of[IO] { case GET -> Root / "ping" =>
      Ok("pong")
    }

  val versionRoutes = HttpRoutes
    .of[IO] {
      // add a new version to the schema
      case req @ POST -> Root / "schema" / id / "version" / "add" =>
        for {
          rawBody <- req.bodyText.compile.string
          newVersion <- new usecases.CreateVersion(repositories)
            .execute(id, rawBody)
          response <- Ok(newVersion)
        } yield response
      // return all available versions
      case req @ GET -> Root / "schema" / id / "version" =>
        for {
          versions <- new usecases.ReadVersionList(repositories).execute(id)
          response <- Ok(versions)
        } yield response
      // return a specific version with its content
      case req @ GET -> Root / "schema" / id / "version" / versionId =>
        for {
          version <- new usecases.ReadVersion(repositories)
            .execute(id, versionId)
          response <- Ok(version)
        } yield response
      // return active version schema
      case req @ GET -> Root / "schema" / id / "version" / "active" =>
        for {
          activeVersion <- new usecases.ReadActiveVersion(repositories)
            .execute(id)
          response <- Ok(activeVersion)
        } yield response
      // update active version to the id
      case req @ PUT -> Root / "schema" / id / "version" / "active" / versionId =>
        for {
          _ <- new usecases.SetActiveVersion(repositories)
            .execute(id, versionId)
          response <- NoContent()
        } yield response
      // remove active version (shutdown the schema)
      case req @ DELETE -> Root / "schema" / id / "version" / "active" =>
        new usecases.UnsetActiveVersion(repositories).execute(id) >> Accepted()
    }

  val schemaRoutes = HttpRoutes
    .of[IO] {
      // create a schema
      case req @ POST -> Root / "schema" =>
        for {
          newSchema <- req.as[CreateSchemaRequest]
          createdSchema <- new usecases.CreateSchema(repositories)
            .execute(newSchema)
          response <- Created(createdSchema)
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
        new usecases.DeleteSchema[IO](repositories).execute(id) >> NoContent()
    }

  val submissionRoutes = HttpRoutes
    .of[IO] {
      // submit answers to a form
      case req @ POST -> Root / "schema" / schemaId / "version" / versionId / "submit" =>
        for {
          rawBody <- req.bodyText.compile.string
          _ <- new usecases.Submit[IO](repositories)
            .execute(schemaId, versionId, rawBody)
          response <- Created()
        } yield response
      // get all submissions associated with a specific version of the form
      case req @ GET -> Root / "schema" / schemaId / "version" / versionId / "submissions" =>
        for {
          answers <- new usecases.GetSubmissionsForVersion[IO](repositories)
            .execute(schemaId, versionId)
          response <- Ok(answers)
        } yield response
    }

  val routes =
    submissionRoutes <+> schemaRoutes <+> versionRoutes <+> infrastructureRoutes

}
