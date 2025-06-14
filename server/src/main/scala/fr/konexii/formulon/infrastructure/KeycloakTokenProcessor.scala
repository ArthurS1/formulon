package fr.konexii.formulon.infrastructure

import pdi.jwt.{JwtAlgorithm, JwtCirce}

import cats.syntax.all._

import java.time.LocalDateTime

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

import fr.konexii.formulon.application._

case class KeycloakToken(
    iss: String,
    sub: String,
    aud: String,
    exp: LocalDateTime,
    nbf: LocalDateTime,
    iat: LocalDateTime,
    role: String
)

object KeycloakTokenProcessor extends TokenProcessor {

  private implicit val decoderForKeycloakToken: Decoder[KeycloakToken] =
    deriveDecoder[KeycloakToken]

  private implicit val encoderForKeycloakToken: Encoder[KeycloakToken] =
    deriveEncoder[KeycloakToken]

  private def validate(token: String, key: String): Option[Json] =
    JwtCirce
      .decodeJson(token, key, Seq(JwtAlgorithm.HS256))
      .toOption

  private def roleFrom(jwt: KeycloakToken): Option[Role] = jwt match {
    case KeycloakToken(
          "http://localhost:8080",
          sub,
          "formulon",
          exp,
          nbf,
          iat,
          "admin"
        ) =>
      Some(Admin())
    case KeycloakToken(
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

  def decodeAndValidate(
      token: String,
      key: String
  ): Either[ErrorMessage, Role] = {
    for {
      json <- Either.fromOption(
        validate(token, key),
        s"Failed to validate JWT token $token."
      )
      jwt <- json
        .as[KeycloakToken]
        .left
        .map(err => s"Failed to parse JWT json ${err.message}.")
      role <- Either.fromOption(
        roleFrom(jwt),
        s"Failed to match the JWT to a role ${jwt.asJson.toString()}."
      )
    } yield role
  }

}
