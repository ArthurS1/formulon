package fr.konexii.formulon.domain

import cats.laws.discipline._
import cats.kernel.laws.discipline._
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.Arbitrary
import org.typelevel.discipline.scalatest.FunSpecDiscipline
import org.scalacheck.Gen
import org.scalacheck.Cogen
import java.util.UUID

class EntityLawTests extends AnyFunSpec with FunSpecDiscipline with Checkers {

  implicit def arbitraryForEntity[T](implicit d: Gen[T]): Arbitrary[Entity[T]] =
    Arbitrary(
      for {
        id <- Gen.uuid
        data <- d
      } yield Entity(id, data)
    )

  implicit val genString: Gen[String] = Gen.stringOf(Gen.asciiChar)

  implicit val genInt: Gen[Int] = Gen.choose(1, 10)

  implicit val cogen: Cogen[Entity[String]] =
    Cogen[(UUID, String)].contramap(mt => (mt.id, mt.data))

  implicit def arbitraryForFunction1[T](implicit a: Arbitrary[Entity[T]], c: Cogen[Entity[T]]): Arbitrary[Entity[T] => Entity[T]] =
    Arbitrary.arbFunction1

  checkAll("functor", FunctorTests[Entity].functor[Int, Int, String])
  checkAll("equality", EqTests[Entity[String]].eqv)

}
