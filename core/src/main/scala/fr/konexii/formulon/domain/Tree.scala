package fr.konexii.formulon.domain

import cats._
import cats.syntax.all._

import java.util.UUID

sealed trait Tree[A]

final case class End[A]() extends Tree[A];
final case class Trunk[A](content: A, next: Tree[A]) extends Tree[A]
final case class Branch[A](
    content: A,
    next: Tree[A],
    out: Tree[A]
) extends Tree[A]

object Tree extends TreeInstances {

  implicit class TreeOps[A](st: Tree[Entity[A]]) {

    def id: Option[UUID] = st match {
      case Trunk(Entity(id, _), next)  => Some(id)
      case Branch(Entity(id, _), _, _) => Some(id)
      case End()                       => None
    }

  }

}

sealed abstract class TreeInstances {

  implicit val functorForTree: Functor[Tree] =
    new Functor[Tree] {
      def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = fa match {
        case End()                => End()
        case Trunk(content, next) => Trunk(f(content), next.map(f))
        case Branch(content, next, out) =>
          Branch(f(content), next.map(f), out.map(f))
      }
    }

  implicit val foldableForTree: Foldable[Tree] =
    new Foldable[Tree] {
      def foldLeft[A, B](fa: Tree[A], b: B)(f: (B, A) => B): B =
        fa match {
          case End()                => b
          case Trunk(content, next) => foldLeft(next, f(b, content))(f)
          case Branch(content, next, out) => {
            val a = foldLeft(next, f(b, content))(f)
            foldLeft(out, a)(f)
          }
        }

      def foldRight[A, B](fa: Tree[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] = fa match {
        case End()                => lb
        case Trunk(content, next) => foldRight(next, f(content, lb))(f)
        case Branch(content, next, out) => {
          val a = foldRight(next, f(content, lb))(f)
          foldRight(out, a)(f)
        }
      }
    }

  implicit val traverseForTree: Traverse[Tree] =
    new Traverse[Tree] {
      def traverse[G[_]: Applicative, A, B](fa: Tree[A])(
          f: A => G[B]
      ): G[Tree[B]] = fa match {
        case End() => Applicative[G].pure(End())
        case Trunk(content, next) => {
          Applicative[G].map2(
            f(content),
            traverse(next)(f)
          ) { case (a, b) =>
            Trunk(a, b)
          }
        }
        case Branch(content, next, out) =>
          Applicative[G].map3(
            f(content),
            traverse(next)(f),
            traverse(out)(f)
          ) { case (a, b, c) =>
            Branch(a, b, c)
          }
      }

      def foldLeft[A, B](fa: Tree[A], b: B)(f: (B, A) => B): B =
        foldableForTree.foldLeft(fa, b)(f)

      def foldRight[A, B](fa: Tree[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] = foldableForTree.foldRight(fa, lb)(f)

    }

  implicit def eqForTree[T: Eq]: Eq[Tree[T]] =
    new Eq[Tree[T]] {
      def eqv(x: Tree[T], y: Tree[T]): Boolean =
        (x, y) match {
          case (End(), End()) => true
          case (Trunk(a, nextA), Trunk(b, nextB)) if a === b =>
            eqv(nextA, nextB)
          case (Branch(a, nextA, outA), Branch(b, nextB, outB)) if a === b =>
            eqv(nextA, nextB) && eqv(outA, outB)
          case _ => false
        }
    }

}
