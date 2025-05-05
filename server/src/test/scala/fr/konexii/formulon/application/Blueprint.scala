package fr.konexii.formulon.application

import cats.effect._

import org.scalatest.funspec.AnyFunSpec

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application._
import fr.konexii.formulon.application.usecases._
import fr.konexii.formulon.application.dtos._
import fr.konexii.formulon.application.repositories._

class BlueprintSuite extends AnyFunSpec {

  implicit val logger: Logger[SyncIO] = Slf4jLogger.getLogger[SyncIO]

  describe("A Blueprint usecase") {

    describe("when executed") {

      it("creates a blueprint") {

        class BlueprintAggregateSpy extends BlueprintAggregate[SyncIO] {
          var calledWith: Option[Entity[Blueprint]] = None

          def get(id: UUID): SyncIO[Entity[Blueprint]] = ???

          def create(schema: Entity[Blueprint]): SyncIO[Entity[Blueprint]] = {
            calledWith = Some(schema)
            SyncIO.pure(schema)
          }

          def delete(schema: Entity[Blueprint]): SyncIO[Unit] = ???

          def update(schema: Entity[Blueprint]): SyncIO[Entity[Blueprint]] = ???

        }

        object RepositoriesSpy extends Repositories[SyncIO] {

          val blueprint = new BlueprintAggregateSpy

          def submission: SubmissionAggregate[SyncIO] = ???

        }

        val result = new CreateBlueprint[SyncIO](RepositoriesSpy)
          .execute(
            CreateBlueprintRequest(name = "test"),
            Org("a", "arthur@icloud.com")
          )
          .unsafeRunSync()

        assertResult(
          Some(Entity(result.id, Blueprint("test", "a", List(), None)))
        )(RepositoriesSpy.blueprint.calledWith)

      }

      it("updates a blueprint") {}

      it("deletes a blueprint") {}

      it("read a blueprint") {}

    }

  }

}
