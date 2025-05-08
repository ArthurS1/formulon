package fr.konexii.formulon.application

import cats.effect._

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpFactory

import org.scalatest.funspec.AnyFunSpec
import fr.konexii.formulon.application.usecases.GetSubmissionsForVersion
import fr.konexii.formulon.application.usecases.Submit

class SubmissionSuite extends AnyFunSpec {

  // Don't forget to replace to get logs
  // Slf4jLogger.getLogger[IO]
  implicit val logger: Logger[SyncIO] = NoOpFactory[SyncIO].getLogger

  describe("A Submission usecase") {

    describe("when executed") {

      describe("in happy path") {

        it("reads all submissions for a given version") {
          val repositories = new RepositoriesSpy

          new GetSubmissionsForVersion(repositories)
            .execute(
              repositories.blueprint.withVersion.id,
              repositories.blueprint.versionA.id
            )
            .unsafeRunSync(): Unit

          assertResult(Some(repositories.blueprint.withVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(Some(repositories.blueprint.versionA))(
            repositories.submission.readAllWith
          )
        }

        it("submits a new answer") {
          val repositories = new RepositoriesSpy

          new Submit(repositories, List())
            .execute(
              repositories.blueprint.withVersion.id,
              repositories.blueprint.versionA.id,
              repositories.submission.basic
            )
            .unsafeRunSync() : Unit

          assertResult(Some(repositories.blueprint.withVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(Some(repositories.submission.basic))(repositories.submission.withCreate.map(_._1.data))
          assertResult(Some(repositories.blueprint.versionA))(repositories.submission.withCreate.map(_._2))
        }

      }

    }

  }

}
