package fr.konexii.formulon.infrastructure

import pdi.jwt.{JwtAlgorithm, JwtCirce}

import java.time.LocalDateTime

import cats.syntax.all._

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

import fr.konexii.formulon.application._

case class Jwt(
    iss: String,
    sub: String,
    aud: String,
    exp: LocalDateTime,
    nbf: LocalDateTime,
    iat: LocalDateTime,
    role: String
)

object Jwt {

  private implicit val decoder: Decoder[Jwt] = deriveDecoder[Jwt]

  private implicit val encoder: Encoder[Jwt] = deriveEncoder[Jwt]

  private def validate(token: String, key: String): Option[Json] =
    JwtCirce
      .decodeJson(token, key, Seq(JwtAlgorithm.HS256))
      .toOption

  private def roleFrom(jwt: Jwt): Option[Role] = jwt match {
    case Jwt(
          "http://localhost:8080",
          sub,
          "formulon",
          exp,
          nbf,
          iat,
          "admin"
        ) =>
      Some(Admin())
    case Jwt(
          "http://localhost:8080",
          sub,
          "formulon",
          exp,
          nbf,
          iat,
          org
        ) =>
      Some(Org(org, sub))
    case _ => None
  }

  type ErrorMessage = String

  def decodeAndValidate(
      token: String,
      key: String
  ): Either[ErrorMessage, Role] = {
    for {
      json <- Either.fromOption(validate(token, key), s"Failed to validate JWT token $token.")
      jwt <- json.as[Jwt].left.map(err => s"Failed to parse JWT json ${err.message}.")
      role <- Either.fromOption(roleFrom(jwt), s"Failed to match the JWT to a role ${jwt.asJson.toString()}.")
    } yield role
  }

}
