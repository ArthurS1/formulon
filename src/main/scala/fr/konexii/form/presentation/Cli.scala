package fr.konexii.form
package presentation

import scala.annotation.tailrec

object Cli {

  sealed trait Conf

  case class Valid(
      val port: String = "8080",
      val ip: String = "0.0.0.0",
      val help: Boolean = false,
      val jdbcUrl: String = "jdbc:postgresql://localhost/konexii",
      val dbUser: String = "form",
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
      case (("--port" | "-p") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(port = v))
      case (("--ip") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(ip = v))
      case (("--jdbcurl") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(jdbcUrl = v))
      case (("--db-user") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbUser = v))
      case (("--db-password") :: v :: xs, conf: Valid) =>
        argParser(xs, conf.copy(dbPass = v))
      case (Nil, conf: Valid) => conf
      case _                  => Invalid()
    }

  val usage = """Form service

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: form-service [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--jdbcurl URL       The JDBC url to connect to the postgres database (default jdbc:postgresql://localhost/konexii)
--db-user USER      The database user to connect as (default "form")
--db-password PASS  The password for this database user (default "test")
--help or -h        Shows this message
"""

}
