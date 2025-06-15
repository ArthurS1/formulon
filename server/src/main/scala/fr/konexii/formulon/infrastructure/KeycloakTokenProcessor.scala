package fr.konexii.formulon.infrastructure

import pdi.jwt.{JwtAlgorithm, JwtCirce}

import cats.syntax.all._

import java.time.LocalDateTime

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

import fr.konexii.formulon.application._

case class RealmAccess(
    roles: List[String]
)

case class KeycloakToken(
    issuer: String,
    subject: String,
    audience: String,
    expires: LocalDateTime,
    notBefore: LocalDateTime,
    issuedAt: LocalDateTime,
    realmAccess: RealmAccess,
    groups: List[String],
    email: String
)

object KeycloakTokenProcessor extends TokenProcessor {

  private implicit val decoderForRealmAccess: Decoder[RealmAccess] =
    deriveDecoder[RealmAccess]

  private implicit val encoderForRealmAccess: Encoder[RealmAccess] =
    deriveEncoder[RealmAccess]

  private implicit val decoderForKeycloakToken: Decoder[KeycloakToken] =
    new Decoder[KeycloakToken] {
      def apply(c: HCursor): Decoder.Result[KeycloakToken] = for {
        iss <- c.downField("iss").as[String]
        sub <- c.downField("sub").as[String]
        aud <- c.downField("aud").as[String]
        exp <- c.downField("exp").as[LocalDateTime]
        nbf <- c.downField("nbf").as[LocalDateTime]
        iat <- c.downField("iat").as[LocalDateTime]
        realmAccess <- c.downField("realm_access").as[RealmAccess]
        groups <- c.downField("groups").as[List[String]]
        email <- c.downField("email").as[String]
      } yield KeycloakToken(
        issuer = iss,
        subject = sub,
        audience = aud,
        expires = exp,
        notBefore = nbf,
        issuedAt = iat,
        realmAccess,
        groups,
        email
      )
    }

  private implicit val encoderForKeycloakToken: Encoder[KeycloakToken] =
    new Encoder[KeycloakToken] {
      def apply(a: KeycloakToken): Json = Json.obj(
        ("iss", Json.fromString(a.issuer)),
        ("sub", Json.fromString(a.subject)),
        ("aud", Json.fromString(a.audience)),
        ("exp", a.expires.asJson),
        ("nbf", a.notBefore.asJson),
        ("iat", a.issuedAt.asJson),
        ("realm_access", a.realmAccess.asJson),
        ("groups", a.groups.asJson),
        ("email", Json.fromString(a.email))
      )
    }

  private def validate(token: String, key: String): Option[Json] =
    JwtCirce
      .decodeJson(token, key, Seq(JwtAlgorithm.RS256))
      .toOption

  private def roleFrom(jwt: KeycloakToken): Either[KeycloakToken, Role] =
    jwt match {
      case KeycloakToken(
            _,
            _,
            _,
            _,
            _,
            _,
            RealmAccess(
              roles
            ),
            _,
            _
          ) if roles.contains("administrator") =>
        Right(Admin())
      case KeycloakToken(
            _,
            _,
            _,
            _,
            _,
            _,
            RealmAccess(
              roles
            ),
            group :: _,
            email
          ) if roles.contains("editor") =>
        Right(Editor(group, email))
      case _ =>
        Left(jwt)
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
      role <- roleFrom(jwt).left.map(incorrectToken =>
        s"Failed to match the JWT to a role ${incorrectToken}."
      )
    } yield role
  }

}
