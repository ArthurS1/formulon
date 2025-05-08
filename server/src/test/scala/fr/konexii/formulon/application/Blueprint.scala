package fr.konexii.formulon.application

import cats.effect._
import cats.syntax.all._

import org.scalatest.funspec.AnyFunSpec

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpFactory

import fr.konexii.formulon.domain.Entity.functorForEntity
import fr.konexii.formulon.application.dtos._
import fr.konexii.formulon.application.usecases._
import fr.konexii.formulon.application.repositories.SubmissionAggregate
import java.util.UUID

class BlueprintSuite extends AnyFunSpec {

  // Don't forget to replace to get logs
  // Slf4jLogger.getLogger[IO]
  implicit val logger: Logger[SyncIO] = NoOpFactory[SyncIO].getLogger

  describe("A Blueprint usecase") {

    describe("when executed") {

      describe("in happy path") {

        it("creates a blueprint") {
          val repositories = new RepositoriesSpy

          val result = new CreateBlueprint[SyncIO](repositories)
            .execute(
              CreateBlueprintRequest(name = "A"),
              Org("a", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(Some(repositories.blueprint.basic.data), "persistance")(
            repositories.blueprint.createdWith.map(_.data)
          )
          assertResult(repositories.blueprint.basic.data)(result.data)
        }

        it("updates a blueprint") {
          val repositories = new RepositoriesSpy
          val newName = "new-name"

          val result = new UpdateBlueprint[SyncIO](repositories)
            .execute(
              UpdateBlueprintRequest(newName),
              repositories.blueprint.basic.id,
              Org("a", "arthur@icloud.com")
            )
            .unsafeRunSync()

          assertResult(
            Some(repositories.blueprint.basic.id),
            "persistance read"
          )(repositories.blueprint.readWith)
          assertResult(
            Some(repositories.blueprint.basic.map(_.copy(name = newName))),
            "persistance update"
          )(
            repositories.blueprint.updatedWith
          )
          assertResult(
            repositories.blueprint.basic.map(_.copy(name = newName)),
            "return value"
          )(result)
        }

        it("deletes a blueprint") {
          val repositories = new RepositoriesSpy

          new DeleteBlueprint[SyncIO](repositories)
            .execute(repositories.blueprint.basic.id, Org("a", "arthur@icloud.com"))
            .unsafeRunSync()

          assertResult(
            Some(repositories.blueprint.basic.id),
            "persistance read"
          )(repositories.blueprint.readWith)
          assertResult(
            Some(repositories.blueprint.basic),
            "persistance delete"
          )(repositories.blueprint.deletedWith)
        }

        it("read a blueprint") {
          object RepositoriesSpy extends Repositories[SyncIO] {
            val blueprint = new BlueprintAggregateSpy
            def submission: SubmissionAggregate[SyncIO] = ???
          }
          val id = UUID.randomUUID

          new ReadBlueprint[SyncIO](RepositoriesSpy)
            .execute(id, Org("a", "arthur@icloud.com"))
            .unsafeRunSync(): Unit

          assertResult(
            Some(id),
            "persistance"
          )(RepositoriesSpy.blueprint.readWith)
        }

      }

    }

  }

}
