package fr.konexii.form

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import cats.effect.std._

import com.comcast.ip4s.{Port, IpAddress}

import org.http4s._
import org.http4s.server.middleware.{Logger => LoggerMidleware, _}
import org.http4s.ember.server._

import fr.konexii.form.presentation.Routes
import fr.konexii.form.presentation.Cli._
import fr.konexii.form.application.Component
import fr.konexii.form.domain.field.Field
import fr.konexii.form.domain.answer.Answer
import fr.konexii.form.application.component._

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
          optionParser(conf) >>=
          serverTupled(conf)
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

  def serverTupled(conf: Valid) = (server(conf) _).tupled

  def server(conf: Valid)(port: Port, ip: IpAddress): IO[ExitCode] = {
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
              conf.dbPass
            )
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

}
