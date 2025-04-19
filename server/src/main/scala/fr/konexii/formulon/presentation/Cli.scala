package fr.konexii.formulon.presentation

import cats.effect._
import cats.effect.std.Console

import java.io._

import scala.annotation.tailrec

object Cli {

  sealed trait Conf

  case class Valid(
      port: Int = 8080,
      ip: String = "0.0.0.0",
      help: Boolean = false,
      dbHost: String = "localhost",
      dbDatabase: String = "formulon",
      dbPort: Int = 5432,
      dbUser: String = "formulon",
      dbPass: String = "test",
      secretKey: String = ""
  ) extends Conf

  case class Invalid(
      val error: String = "Unknown configuration error"
  ) extends Conf

  @tailrec
  def argParser(args: List[String], acc: Conf): Conf =
    (args, acc) match {
      case (("--help" | "-h") :: xs, conf: Valid) =>
        argParser(xs, conf.copy(help = true))
      case (("--ip") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(ip = v))
      case (("--db-host") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbHost = v))
      case (("--db-database") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbDatabase = v))
      case (("--db-user") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbUser = v))
      case (("--db-password") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbPass = v))
      case (("--port" | "-p") :: v :: _, conf: Valid) =>
        v.toIntOption
          .map((port: Int) => conf.copy(port = port))
          .getOrElse(Invalid("Server port is incorrect."))
      case (("--db-port") :: v :: xs, conf: Valid) =>
        argParser(
          xs,
          v.toIntOption
            .map((port: Int) => conf.copy(dbPort = port))
            .getOrElse(Invalid("Database port is incorrect."))
        )
      case (("--key") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(secretKey = v))
      case (Nil, conf: Valid) => conf
      case _                  => Invalid()
    }

  def banner(): IO[Unit] = {
    val resource = Resource.fromAutoCloseable(IO {
      val stream = getClass.getClassLoader.getResourceAsStream("banner.txt")
      require(stream != null, s"Banner file not found.")
      new BufferedReader(new InputStreamReader(stream))
    })

    resource.use { f =>
      def loop(): IO[Unit] =
        IO(f.readLine()).flatMap {
          case null => IO.unit
          case line => Console[IO].println(line) >> loop()
        }
      loop()
    }
  }

  val usage = """Formulon

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: formulon [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--db-user USER      The DBMS user to connect as (default "formulon")
--db-password PASS  The password for this DBMS user (default "test")
--db-host HOST      The host of the DBMS (default "localhost")
--db-port PASS      The port of the DBMS (default "5432")
--db-database DB    The database to connect to (default "formulon")
--help or -h        Shows this message
"""

}
