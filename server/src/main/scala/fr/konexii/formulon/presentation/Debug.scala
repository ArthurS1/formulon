package fr.konexii.formulon.presentation

import io.circe._

import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityEncoder._

object Debug {

  def debugResponse(msg: String) =
    InternalServerError(Json.obj(("error", Json.fromString(msg))))

}
