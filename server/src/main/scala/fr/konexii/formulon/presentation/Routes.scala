package fr.konexii.formulon.presentation

import cats.data._
import cats.effect._
import cats.syntax.all._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization

import io.circe._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.dtos._
import fr.konexii.formulon.presentation.Serialization._
import fr.konexii.formulon.infrastructure.Jwt

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Routes(
    repositories: Repositories[IO],
    plugins: List[Plugin],
    secretKey: String
) {

  implicit def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  implicit val encoder: Encoder[Submission] = encoderForSubmission(plugins)

  val roleMiddleware: AuthMiddleware[IO, Role] =
    AuthMiddleware(getRole(secretKey))

  val infrastructureRoutes = HttpRoutes
    .of[IO] { case GET -> Root / "ping" =>
      Ok("pong")
    }

  val versionRoutes = HttpRoutes
    .of[IO] {
      // return active version schema
      case GET -> Root / "schema" / UUIDVar(id) / "version" / "active" =>
        for {
          activeVersion <- new usecases.ReadActiveVersion(repositories)
            .execute(id)
          response <- Ok(activeVersion)
        } yield response
    }

  val authedVersionRoutes: AuthedRoutes[Role, IO] =
    AuthedRoutes.of {
      // add a new version to the schema
      case authedReq @ POST -> Root / "schema" / UUIDVar(
            id
          ) / "version" / "add" as role =>
        for {
          rawBody <- authedReq.req.bodyText.compile.string
          newVersion <- new usecases.CreateVersion(repositories)
            .execute(id, rawBody)
          response <- Ok(newVersion)
        } yield response
      // return all available versions
      case GET -> Root / "schema" / UUIDVar(id) / "version" as role =>
        for {
          versions <- new usecases.ReadVersionList(repositories).execute(id)
          response <- Ok(versions)
        } yield response
      // return a specific version with its content
      case GET -> Root / "schema" / UUIDVar(id) / "version" / UUIDVar(
            versionId
          ) as role =>
        for {
          version <- new usecases.ReadVersion(repositories)
            .execute(id, versionId)
          response <- Ok(version)
        } yield response
      // update active version to the id
      case PUT -> Root / "schema" / UUIDVar(
            id
          ) / "version" / "active" / UUIDVar(versionId) as role =>
        for {
          _ <- new usecases.SetActiveVersion(repositories)
            .execute(id, versionId)
          response <- NoContent()
        } yield response
      // remove active version (shutdown the schema)
      case DELETE -> Root / "schema" / UUIDVar(id) / "version" / "active" as role =>
        new usecases.UnsetActiveVersion(repositories).execute(id) >> Accepted()
    }

  val authedBlueprintRoutes: AuthedRoutes[Role, IO] =
    AuthedRoutes.of {
      // create a blueprint
      case authedReq @ POST -> Root / "schema" as Org(orgName, _) =>
        for {
          newSchema <- authedReq.req.as[CreateSchemaRequest]
          createdSchema <- new usecases.CreateSchema(repositories)
            .execute(newSchema)
          response <- Created(createdSchema)
        } yield response
      // given an id, get the active version of a blueprint
      case GET -> Root / "schema" / UUIDVar(id) as _ =>
        for {
          schema <- new usecases.ReadBlueprint[IO](repositories).execute(id)
          response <- Ok(schema)
        } yield response
      // update the blueprint
      case authedReq @ PUT -> Root / "schema" / UUIDVar(id) as _ =>
        for {
          update <- authedReq.req.as[UpdateSchemaRequest]
          updatedSchema <- new usecases.UpdateSchema[IO](repositories)
            .execute(update, id)
          response <- Ok(updatedSchema)
        } yield response
      // delete the blueprint
      case DELETE -> Root / "schema" / UUIDVar(id) as Org(orgName, _) =>
        new usecases.DeleteSchema[IO](repositories).execute(id) >> NoContent()
    }

  val submissionRoutes = HttpRoutes
    .of[IO] {
      // submit answers to a form
      case req @ POST -> Root / "schema" / UUIDVar(
            schemaId
          ) / "version" / UUIDVar(versionId) / "submit" =>
        for {
          rawBody <- req.bodyText.compile.string
          _ <- new usecases.Submit[IO](repositories)
            .execute(schemaId, versionId, rawBody)
          response <- Created()
        } yield response
      // get all submissions associated with a specific version of the form
      case GET -> Root / "schema" / UUIDVar(schemaId) / "version" / UUIDVar(
            versionId
          ) / "submissions" =>
        for {
          answers <- new usecases.GetSubmissionsForVersion[IO](repositories)
            .execute(schemaId, versionId)
          response <- Ok(answers)
        } yield response
    }

  val routes = submissionRoutes <+>
    roleMiddleware(authedBlueprintRoutes) <+>
    roleMiddleware(authedVersionRoutes) <+>
    versionRoutes <+>
    infrastructureRoutes

  type OptionTIO[A] = OptionT[IO, A]

  def getRole(key: String): Kleisli[OptionTIO, Request[IO], Role] =
    Kleisli(request =>
      request.headers.get[Authorization] match {
        case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
          Jwt.decodeAndValidate(token, key) match {
            case Left(value) =>
              OptionT.liftF(
                Logger[IO].warn(s"Authorization: $value")
              ) >> OptionT.none[IO, Role]
            case Right(value) => OptionT.liftF(IO(value))
          }
        case _ =>
          OptionT.liftF(
            Logger[IO].warn(
              "Failure to find the credential within the Authorization header of the request."
            )
          ) >> OptionT.none[IO, Role]

      }
    )

}
