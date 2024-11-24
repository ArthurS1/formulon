package fr.konexii.form
package presentation

import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityEncoder._
import io.circe._

object Debug {

  def debugResponse(msg: String) =
    InternalServerError(Json.obj(("error", Json.fromString(msg))))

}
