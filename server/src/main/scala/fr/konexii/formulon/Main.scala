package fr.konexii.formulon

import scala.jdk.CollectionConverters._

import cats.data._
import cats.implicits._
import cats.effect._
import cats.effect.std._

import com.comcast.ip4s.{Port, IpAddress}

import org.http4s._
import org.http4s.ember.server._
import org.http4s.server.middleware.{Logger => LoggerMidleware, _}
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization

import fr.konexii.formulon.presentation.Routes
import fr.konexii.formulon.presentation.Cli._
import fr.konexii.formulon.application.Plugin
import fr.konexii.formulon.builtins.Text.Text
import fr.konexii.formulon.application.Role
import fr.konexii.formulon.infrastructure.Jwt

import java.util.ServiceLoader

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  implicit def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    argParser(args, Valid()) match {
      case conf: Valid if conf.help == true =>
        Console[IO]
          .println(usage)
          .map(_ => ExitCode(0))
      case conf: Valid =>
        infos(conf) >>
          (pluginsLoader(), optionParser(conf)).flatMapN {
            case (plugins, (port, ip)) =>
              server(conf, plugins ++ builtinPlugins, port, ip)
          }
      case Invalid(msg) =>
        Console[IO]
          .errorln(s"Configuration error: $msg")
          .map(_ => ExitCode(1))
    }

  def infos(conf: Valid): IO[Unit] =
    banner().handleErrorWith(_ =>
      IO.println("Formulon started. Failed to load banner.")
    ) >>
      IO.println(s"port: ${conf.port}") >>
      IO.println(s"ip: ${conf.ip}")

  def optionParser(conf: Valid): IO[(Port, IpAddress)] =
    (Port.fromInt(conf.port), IpAddress.fromString(conf.ip)) match {
      case (Some(port), Some(ip)) => IO((port, ip))
      case _ => IO.raiseError(new RuntimeException("Failed to parse options."))
    }

  def pluginsLoader(): IO[List[Plugin]] =
    IO.delay(
      ServiceLoader.load(classOf[Plugin]).iterator().asScala.toList
    ).flatMap(plugins =>
      (if (plugins.isEmpty)
         IO.println(
           s"No external plugins loaded. Builtins: ${builtinPlugins.map(_.name).mkString(", ")}"
         )
       else
         IO.println(
           s"Loaded plugins: ${(plugins ++ builtinPlugins).map(_.name).mkString(", ")}."
         ))
        >> IO(plugins)
    )

  lazy val builtinPlugins: List[Plugin] = List(Text())

  def server(
      conf: Valid,
      plugins: List[Plugin],
      port: Port,
      ip: IpAddress
  ): IO[ExitCode] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ip)
      .withPort(port)
      .withHttpApp(
        middleware(
          new Routes(
            new infrastructure.PostgresRepositories[IO](
              conf.dbHost,
              conf.dbPort,
              conf.dbDatabase,
              conf.dbUser,
              conf.dbPass,
              plugins
            ),
            plugins
          ).routes
        ).orNotFound
      )
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  def middleware(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    LoggerMidleware.httpRoutes[IO](
      logHeaders = true,
      logBody = true
    )(
      ErrorHandling.Custom.recoverWith(routes) { case e: Exception =>
        OptionT.liftF(
          for {
            _ <- Logger[IO].error(e)(
              s"""An error was never caught : ${e.toString}"""
            )
          } yield Response[IO](status = Status.InternalServerError)
        )
      }
    )

  val roleMiddleware: AuthMiddleware[IO, Role] =
    AuthMiddleware(getRole)

  val key: String = ???

  type OptionTIO[A] = OptionT[IO, A]

  val getRole: Kleisli[OptionTIO, Request[IO], Role] =
    Kleisli(request =>
      request.headers.get[Authorization] match {
        case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
          OptionT.fromOption(Jwt.decodeJwt(token, key))
        case _ => {
          for {
            _ <- OptionT.liftF(
              Logger[IO].warn(
                "Failure to find the credential within the Authorization header of the request."
              )
            )
            none <- OptionT.none[IO, Role]
          } yield none
        }
      }
    )

}
