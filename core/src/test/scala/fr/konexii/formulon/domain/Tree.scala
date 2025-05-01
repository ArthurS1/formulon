package fr.konexii.formulon.domain

import cats.kernel.laws.discipline._
import org.scalatest.funspec.AnyFunSpec
import org.typelevel.discipline.scalatest.FunSpecDiscipline
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Cogen
import cats.laws.discipline.FunctorTests
import cats.laws.discipline.FoldableTests
import cats.laws.discipline.TraverseTests

class TreeLawTests extends AnyFunSpec with FunSpecDiscipline with Checkers {

  def tree[T](depth: Int)(implicit a: Gen[T]): Gen[Tree[T]] = {
    lazy val branch: Gen[Tree[T]] = for {
      next <- tree[T](depth - 1)
      out <- tree[T](depth - 1)
      data <- a
    } yield Branch(data, next, out)

    lazy val trunk: Gen[Tree[T]] = for {
      next <- tree[T](depth - 1)
      data <- a
    } yield Trunk(data, next)

    if (depth <= 0)
      Gen.const[Tree[T]](End())
    else
      Gen.oneOf(
        Gen.const[Tree[T]](End()),
        Gen.lzy(branch),
        Gen.lzy(trunk)
      )
  }

  implicit def arbitraryForTree[T](implicit a: Gen[T]): Arbitrary[Tree[T]] =
    Arbitrary(tree[T](30))

  implicit val genString: Gen[String] = Gen.stringOfN(10, Gen.alphaChar)

  implicit val genInt: Gen[Int] = Gen.choose(0, 100)

  implicit def cogen[T: Cogen]: Cogen[Tree[T]] =
    Cogen[Option[T]].contramap(mt =>
      mt match {
        case Branch(content, next, out) => Some(content)
        case End()                      => None
        case Trunk(content, next)       => Some(content)
      }
    )

  implicit def arbitraryForFunction1[T](implicit
      a: Arbitrary[Tree[T]],
      c: Cogen[Tree[T]]
  ): Arbitrary[Tree[T] => Tree[T]] = Arbitrary.arbFunction1

  implicit def z[T](implicit g: Gen[T]): Arbitrary[Tree[Option[T]]] = arbitraryForTree(Gen.option(g))

  checkAll("equality", EqTests[Tree[String]].eqv)
  checkAll("functor", FunctorTests[Tree].functor[String, Int, Int])
  checkAll("foldable", FoldableTests[Tree].foldable[Int, Int])
  checkAll("traverse", TraverseTests[Tree].traverse[Int, Int, Int, Int, Option, Option])
}
