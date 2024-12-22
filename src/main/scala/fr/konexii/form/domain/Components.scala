package fr.konexii.form
package domain

/*
 * This part is still work in progress.
 * It is intended to represent the schema of a form - this means a model of
 * fields and their relations without a way to storing their value.
 */

// A block is any element of the form tree

// A condition block creates a new branch in the tree based on a condition

/*
 * I do not know what is happening, metals looses its shit when I uncomment this
case class Branch(
    condition: ConditionalOperator,
    ifTrue: Block,
    ifFalse: Block
) extends Block

trait ConditionalOperator

case class Equal(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class NotEqual(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class InferiorOrEqual(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class Inferior(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class SuperiorOrEqual(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class Superior(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator

case class And(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator
case class Or(
    rhs: ConditionalOperator,
    lhs: ConditionalOperator
) extends ConditionalOperator

trait ConditionalValue extends ConditionalOperator

case class StringLiteral(value: String) extends ConditionalValue
case class StringListLiteral(value: List[String]) extends ConditionalValue

case class Property(blockId: String) extends ConditionalValue

// The end block marks the end of the tree
*/

/*
// A group holds and presents Fields

case class VisualGroup(
    fields: List[Field]
) extends Block

// A statement block is a single field

case class Field(
    title: String,
    required: Boolean,
    attributes: Attribute,
    next: Block
) extends Block

// A field contains any of the attribute secific properties

trait Attribute

case class Text(
    tooltip: Option[String] = None
) extends Attribute

case class Numeric(
    tooltip: Option[String],
    upperLimit: Int,
    lowerLimit: Int
) extends Attribute

case class SingleChoice(
    choices: List[String]
) extends Attribute

case class MultipleChoice(
    choices: List[String]
) extends Attribute*/
