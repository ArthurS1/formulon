package fr.konexii.formulon.infrastructure

import pdi.jwt.{JwtAlgorithm, JwtCirce}

import fr.konexii.formulon.application.Role

case class Jwt(
    iss: String,
    sub: String,
    aud: String,
    exp: String,
    nbf: String,
    iat: String,
    jti: String,
    role: String
)

object Jwt {

  def decodeJwt(token: String, key: String): Option[Role] =
    JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.HS256)).map(???).toOption

}
