package fr.konexii.formulon.presentation

import cats.data._
import cats.effect._
import cats.syntax.all._

import org.http4s.{Entity => _, _}
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

  implicit val answerEncoder: Encoder[Answer] = encoderForAnswer(plugins)
  implicit val fieldEncoder: Encoder[FieldWithMetadata] =
    encoderForFieldWithMetadata(plugins)
  implicit val fieldDecoder: Decoder[FieldWithMetadata] =
    decoderForFieldWithMetadata(plugins)
  implicit val answerDecoder: Decoder[Answer] = decoderForAnswer(plugins)

  val roleMiddleware: AuthMiddleware[IO, Role] =
    AuthMiddleware(getRole(secretKey))

  val infrastructureRoutes = HttpRoutes
    .of[IO] { case GET -> Root / "ping" =>
      Ok("pong")
    }

  val versionRoutes = HttpRoutes
    .of[IO] {
      // return active version blueprint
      case GET -> Root / "blueprint" / UUIDVar(id) / "version" / "active" =>
        for {
          activeVersion <- new usecases.ReadActiveVersion(repositories)
            .execute(id)
          response <- Ok(activeVersion)
        } yield response
    }

  val authedVersionRoutes: AuthedRoutes[Role, IO] =
    AuthedRoutes.of {
      // add a new version to the blueprint
      case authedReq @ POST -> Root / "blueprint" / UUIDVar(
            id
          ) / "version" / "add" as role =>
        for {
          content <- authedReq.req.as[Tree[Entity[FieldWithMetadata]]]
          newVersion <- new usecases.CreateVersion(repositories)
            .execute(id, content, role)
          response <- Ok(newVersion)
        } yield response
      // return all available versions
      case GET -> Root / "blueprint" / UUIDVar(id) / "version" as role =>
        for {
          versions <- new usecases.ReadVersionList(repositories)
            .execute(id, role)
          response <- Ok(versions)
        } yield response
      // return a specific version with its content
      case GET -> Root / "blueprint" / UUIDVar(id) / "version" / UUIDVar(
            versionId
          ) as role =>
        for {
          version <- new usecases.ReadVersion(repositories)
            .execute(id, versionId, role)
          response <- Ok(version)
        } yield response
      // update active version to the id
      case PUT -> Root / "blueprint" / UUIDVar(
            id
          ) / "version" / "active" / UUIDVar(versionId) as role =>
        for {
          _ <- new usecases.SetActiveVersion(repositories)
            .execute(id, versionId, role)
          response <- NoContent()
        } yield response
      // remove active version (equivalent to turning off the form submissions)
      case DELETE -> Root / "blueprint" / UUIDVar(
            id
          ) / "version" / "active" as role =>
        new usecases.UnsetActiveVersion(repositories)
          .execute(id, role) >> Accepted()
    }

  val authedBlueprintRoutes: AuthedRoutes[Role, IO] =
    AuthedRoutes.of {
      // create a blueprint
      case authedReq @ POST -> Root / "blueprint" as role =>
        for {
          newblueprint <- authedReq.req.as[CreateBlueprintRequest]
          createdblueprint <- new usecases.CreateBlueprint(repositories)
            .execute(newblueprint, role)
          response <- Created(createdblueprint)
        } yield response
      // given an id, get the active version of a blueprint
      case GET -> Root / "blueprint" / UUIDVar(id) as role =>
        for {
          blueprint <- new usecases.ReadBlueprint[IO](repositories)
            .execute(id, role)
          response <- Ok(blueprint)
        } yield response
      // update the blueprint
      case authedReq @ PUT -> Root / "blueprint" / UUIDVar(id) as role =>
        for {
          update <- authedReq.req.as[UpdateBlueprintRequest]
          updatedblueprint <- new usecases.UpdateBlueprint[IO](repositories)
            .execute(update, id, role)
          response <- Ok(updatedblueprint)
        } yield response
      // delete the blueprint
      case DELETE -> Root / "blueprint" / UUIDVar(id) as role =>
        new usecases.DeleteBlueprint[IO](repositories)
          .execute(id, role) >> NoContent()
    }

  val submissionRoutes = HttpRoutes
    .of[IO] {
      // submit answers to a form
      case req @ POST -> Root / "blueprint" / UUIDVar(
            blueprintId
          ) / "version" / UUIDVar(versionId) / "submit" =>
        for {
          submission <- req.as[Submission]
          _ <- new usecases.Submit[IO](repositories, plugins)
            .execute(blueprintId, versionId, submission)
          response <- Created()
        } yield response
      // get all submissions associated with a specific version of the form
      case GET -> Root / "blueprint" / UUIDVar(
            blueprintId
          ) / "version" / UUIDVar(
            versionId
          ) / "submissions" =>
        for {
          answers <- new usecases.GetSubmissionsForVersion[IO](repositories)
            .execute(blueprintId, versionId)
          response <- Ok(answers)
        } yield response
    }

  val routes = infrastructureRoutes <+>
    versionRoutes <+>
    submissionRoutes <+>
    roleMiddleware(authedBlueprintRoutes) <+>
    roleMiddleware(authedVersionRoutes)

  type OptionTIO[A] = OptionT[IO, A]

  def getRole(key: String): Kleisli[OptionTIO, Request[IO], Role] =
    Kleisli(request => {
      println(s"YO ${request.uri.toString}")
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
    })

}
