package fr.konexii.form.application.utils

import cats._

import java.util.UUID

object uuid {

  implicit class StringOps(s: String) {

    def toUuid[F[_]: MonadThrow]: F[UUID] =
      MonadThrow[F].catchNonFatal(UUID.fromString(s))

  }

}
