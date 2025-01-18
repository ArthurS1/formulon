package fr.konexii.form
package domain

import cats._
import cats.syntax.all._

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

