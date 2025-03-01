package fr.konexii.form.domain

import cats._
import cats.syntax.all._

import java.util.UUID

sealed trait SchemaTree[A]

final case class End[A]() extends SchemaTree[A];
final case class Trunk[A](content: A, next: SchemaTree[A]) extends SchemaTree[A]
final case class Branch[A](
    content: A,
    next: SchemaTree[A],
    out: SchemaTree[A]
) extends SchemaTree[A]

object SchemaTree extends SchemaTreeInstances {

  implicit class SchemaTreeOps[A](st: SchemaTree[Entity[A]]) {

    def id: Option[UUID] = st match {
      case Trunk(Entity(id, _), next)  => Some(id)
      case Branch(Entity(id, _), _, _) => Some(id)
      case End()                       => None
    }

  }

}

sealed abstract class SchemaTreeInstances {

  implicit val functorForSchemaTree: Functor[SchemaTree] =
    new Functor[SchemaTree] {
      def map[A, B](fa: SchemaTree[A])(f: A => B): SchemaTree[B] = fa match {
        case End()                => End()
        case Trunk(content, next) => Trunk(f(content), next.map(f))
        case Branch(content, next, out) =>
          Branch(f(content), next.map(f), out.map(f))
      }
    }

  implicit val foldableForSchemaTree: Foldable[SchemaTree] =
    new Foldable[SchemaTree] {
      def foldLeft[A, B](fa: SchemaTree[A], b: B)(f: (B, A) => B): B =
        fa match {
          case End()                => b
          case Trunk(content, next) => foldLeft(next, f(b, content))(f)
          case Branch(content, next, out) => {
            val a = foldLeft(next, f(b, content))(f)
            foldLeft(out, a)(f)
          }
        }

      def foldRight[A, B](fa: SchemaTree[A], lb: Eval[B])(
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

  implicit val traverseForSchemaTree: Traverse[SchemaTree] =
    new Traverse[SchemaTree] {
      def traverse[G[_]: Applicative, A, B](fa: SchemaTree[A])(
          f: A => G[B]
      ): G[SchemaTree[B]] = fa match {
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

      def foldLeft[A, B](fa: SchemaTree[A], b: B)(f: (B, A) => B): B =
        foldableForSchemaTree.foldLeft(fa, b)(f)

      def foldRight[A, B](fa: SchemaTree[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] = foldableForSchemaTree.foldRight(fa, lb)(f)

    }

}
