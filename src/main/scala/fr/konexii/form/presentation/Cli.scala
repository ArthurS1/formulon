package fr.konexii.form
package presentation

import scala.annotation.tailrec

object Cli {

  sealed trait Conf

  case class Valid(
      val port: Int = 8080,
      val ip: String = "0.0.0.0",
      val help: Boolean = false,
      val dbHost: String = "localhost",
      val dbDatabase: String = "formulon",
      val dbPort: Int = 5432,
      val dbUser: String = "server",
      val dbPass: String = "test"
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
      case (("--port" | "-p") :: v :: xs, conf: Valid) =>
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
      case (Nil, conf: Valid) => conf
      case _                  => Invalid()
    }

  val usage = """Formulon

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: formulon [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--db-user USER      The DBMS user to connect as (default "form")
--db-password PASS  The password for this DBMS user (default "test")
--db-host HOST      The host of the DBMS (default "localhost")
--db-port PASS      The port of the DBMS (default "5432")
--db-database DB    The database to connect to (default "formulon")
--help or -h        Shows this message
"""

}
