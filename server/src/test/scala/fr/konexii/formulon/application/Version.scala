package fr.konexii.formulon.application

import cats.syntax.all._
import cats.effect._

import org.scalatest.funspec.AnyFunSpec

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpFactory

import fr.konexii.formulon.domain.Entity._
import fr.konexii.formulon.application.usecases._
import fr.konexii.formulon.domain._

class VersionSuite extends AnyFunSpec {

  // Don't forget to replace to get logs
  // Slf4jLogger.getLogger[IO]
  implicit val logger: Logger[SyncIO] = NoOpFactory[SyncIO].getLogger

  describe("A Version usecase") {

    describe("when executed") {

      describe("in happy path") {

        it("reads the active version") {
          val repositories = new RepositoriesSpy

          new ReadActiveVersion(repositories)
            .execute(repositories.blueprint.withActiveVersion.id)
            .unsafeRunSync(): Unit

          assertResult(
            Some(repositories.blueprint.withActiveVersion.id),
            "persistance"
          )(repositories.blueprint.readWith)
        }

        it("reads the list of versions") {
          val repositories = new RepositoriesSpy

          val result = new ReadVersionList(repositories)
            .execute(
              repositories.blueprint.withVersion.id,
              Org("b", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(Some(repositories.blueprint.withVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(repositories.blueprint.withVersion.data.versions)(result)
        }

        it("reads a single version") {
          val repositories = new RepositoriesSpy

          val result = new ReadVersion(repositories)
            .execute(
              repositories.blueprint.withVersion.id,
              repositories.blueprint.withVersion.data.versions.head.id,
              Org("b", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(Some(repositories.blueprint.withVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(repositories.blueprint.withVersion.data.versions.head)(
            result
          )
        }

        it("removes the active version") {
          val repositories = new RepositoriesSpy

          new UnsetActiveVersion(repositories)
            .execute(
              repositories.blueprint.withActiveVersion.id,
              Org("c", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(Some(repositories.blueprint.withActiveVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(
            Some(
              repositories.blueprint.withActiveVersion.map(
                _.copy(active = None)
              )
            )
          )(
            repositories.blueprint.updatedWith
          )
        }

        it("sets the active version") {
          val repositories = new RepositoriesSpy

          new SetActiveVersion(repositories)
            .execute(
              repositories.blueprint.withActiveVersion.id,
              repositories.blueprint.withActiveVersion.data.versions.last.id,
              Org("c", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(Some(repositories.blueprint.withActiveVersion.id))(
            repositories.blueprint.readWith
          )
          assertResult(
            Some(
              repositories.blueprint.withActiveVersion.map(
                _.copy(active =
                  Some(
                    repositories.blueprint.withActiveVersion.data.versions.last
                  )
                )
              )
            )
          )(
            repositories.blueprint.updatedWith
          )
        }

        it("creates a new version") {
          val repositories = new RepositoriesSpy

          new CreateVersion(repositories)
            .execute(
              repositories.blueprint.basic.id,
              End(),
              Org("a", "arthur@icloud.com")
            )
            .unsafeRunSync(): Unit

          assertResult(Some(repositories.blueprint.basic.id))(
            repositories.blueprint.readWith
          )
          assertResult(
            Some(End())
          )(
            repositories.blueprint.updatedWith.map(
              _.data.versions.head.data.content
            )
          )
        }

      }

    }

  }

}
