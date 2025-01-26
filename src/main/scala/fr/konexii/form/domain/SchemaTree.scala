package fr.konexii.form
package domain

import cats._
import cats.syntax.all._

import java.util.UUID
import scala.collection.immutable

/*
 * Example of input/output jsons.
 *
 * "<uuid>" {
 *  "type": "branch"
 *  "cond": "<uuid>.value == "test""
 *  "ifTrue": <uuid>
 *  "ifFalse": <uuid>
 * }
 * "<uuid>" {
 *  "type": "block"
 *  "field": {
 *    "type": "text"
 *    "title": "hello"
 *    "required": true
 *    "data" {
 *      "tooltip": "please insert hello"
 *      ...
 *    }
 *  }
 *  "next": <uuid> (optional if it does not exist, this is the end of the chain)
 * }
 */

sealed trait SchemaTree[T]

final case class Block[T](data: Entity[T], next: Option[SchemaTree[T]])
    extends SchemaTree[T]
final case class Branch[T](
    condition: String,
    choices: Entity[(Option[SchemaTree[T]], Option[SchemaTree[T]])]
) extends SchemaTree[T]

sealed abstract private[domain] class SchemaTreeInstances {

  implicit val functorForSchemaTree: Functor[SchemaTree] =
    new Functor[SchemaTree] {
      def map[A, B](fa: SchemaTree[A])(f: A => B): SchemaTree[B] = fa match {
        case Block(data, next) => Block(data.map(f(_)), next.map(_.map(f)))
        case Branch(cond, choices) =>
          Branch(
            cond,
            choices.map { case (left, right) =>
              (left.map(_.map(f)), right.map(_.map(f)))
            }
          )
      }
    }

  implicit val foldableForSchemaTree: Foldable[SchemaTree] =
    new Foldable[SchemaTree] {
      def foldLeft[A, B](fa: SchemaTree[A], b: B)(f: (B, A) => B): B =
        fa match {
          case Block(Entity(_, data), None) => f(b, data)
          case Block(Entity(_, data), Some(next)) =>
            foldLeft(next, f(b, data))(f)
          case Branch(_, Entity(_, (Some(left), Some(right)))) => {
            val a = foldLeft(left, b)(f)
            foldLeft(right, a)(f)
          }
          case Branch(_, Entity(_, (Some(left), None))) =>
            foldLeft(left, b)(f)
          case Branch(_, Entity(_, (None, Some(right)))) =>
            foldLeft(right, b)(f)
          case Branch(_, _) =>
            b
        }

      def foldRight[A, B](fa: SchemaTree[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] =
        fa match {
          case Block(Entity(_, data), None) => f(data, lb)
          case Block(Entity(_, data), Some(next)) =>
            foldRight(next, f(data, lb))(f)
          case Branch(_, Entity(_, (Some(left), Some(right)))) => {
            val a = foldRight(right, lb)(f)
            foldRight(left, a)(f)
          }
          case Branch(_, Entity(_, (Some(left), None))) =>
            foldRight(left, lb)(f)
          case Branch(_, Entity(_, (None, Some(right)))) =>
            foldRight(right, lb)(f)
          case Branch(_, _) =>
            lb
        }
    }

}
