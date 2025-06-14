package fr.konexii.formulon.application

import java.util.UUID

import cats.syntax.all._
import cats.effect._

import fr.konexii.formulon.domain._
import fr.konexii.formulon.application.repositories.BlueprintAggregate
import fr.konexii.formulon.application.repositories.SubmissionAggregate

class RepositoriesSpy extends Repositories[SyncIO] {
  val blueprint = new BlueprintAggregateSpy
  val submission = new SubmissionAggregateSpy
}

object A {

  val aId = UUID.randomUUID

}


class BlueprintAggregateSpy extends BlueprintAggregate[SyncIO] {

  val versionA =
    Entity(
      UUID.randomUUID,
      Version
        .apply[SyncIO](
          Trunk(
            Entity(
              A.aId,
              new FieldWithMetadata("title", true, TestField())
            ),
            End()
          )
        )
        .unsafeRunSync()
    )
  val versionB =
    Entity(UUID.randomUUID, Version.apply[SyncIO](End()).unsafeRunSync())
  val basic =
    Entity(UUID.randomUUID, Blueprint("A", "a", List(), None))
  val withVersion =
    Entity(UUID.randomUUID, Blueprint("B", "b", List(versionA), None))
  val withActiveVersion =
    Entity(
      UUID.randomUUID,
      Blueprint("C", "c", List(versionA, versionB), Some(versionA))
    )
  val list = List(basic, withVersion, withActiveVersion)

  var readWith: Option[UUID] = None
  def get(id: UUID): SyncIO[Entity[Blueprint]] = {
    readWith = Some(id)
    SyncIO.pure(list.find(e => e.id === id).getOrElse(basic))
  }

  def getAll(): SyncIO[List[Entity[Blueprint]]] = SyncIO.pure(list)

  var createdWith: Option[Entity[Blueprint]] = None
  def create(blueprint: Entity[Blueprint]): SyncIO[Entity[Blueprint]] = {
    createdWith = Some(blueprint)
    SyncIO.pure(blueprint)
  }

  var deletedWith: Option[Entity[Blueprint]] = None
  def delete(blueprint: Entity[Blueprint]): SyncIO[Unit] = {
    deletedWith = Some(blueprint)
    SyncIO.unit
  }

  var updatedWith: Option[Entity[Blueprint]] = None
  def update(blueprint: Entity[Blueprint]): SyncIO[Entity[Blueprint]] = {
    updatedWith = Some(blueprint)
    SyncIO.pure(blueprint)
  }


}

class SubmissionAggregateSpy extends SubmissionAggregate[SyncIO] {

  val answer = TestAnswer()
  val basic = Submission(List())
  val withVersion = Submission(List(Entity(A.aId, answer)))

  var withCreate: Option[(Entity[Submission], Entity[Version])] = None
  def create(
      submission: Entity[Submission],
      version: Entity[Version]
  ): SyncIO[Entity[Submission]] = {
    withCreate = Some((submission, version))
    SyncIO.pure(submission)
  }

  var readAllWith: Option[Entity[Version]] = None
  def getAll(version: Entity[Version]): SyncIO[List[Entity[Submission]]] = {
    readAllWith = Some(version)
    SyncIO.pure(List())
  }

}
