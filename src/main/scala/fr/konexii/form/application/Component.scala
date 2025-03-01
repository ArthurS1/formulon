package fr.konexii.form.application

import cats.syntax.all._
import cats.data._

import io.circe._

import fr.konexii.form.domain.field.Field
import fr.konexii.form.domain.answer.Answer

/*
 * A component is the schema field + the answer structure + the string type
 * loaded at runtime. Basically a plugin.
 */
trait Component[+A <: Field, +B <: Answer] {

  def typeStr: String

  implicit def decoderForField: Decoder[A]
  implicit def encoderForField: Encoder[A]

  implicit def decoderForAnswer: Decoder[B]
  implicit def encoderForAnswer: Encoder[B]

  def validate(aField: A, anAnswer: B): ValidatedNec[Throwable, B]

}

object Component {

  type Plugins = NonEmptySeq[Component[Field, Answer]]

  def forType(typeStr: String): Option[Component[Field, Answer]] =
    registered.find(_.typeStr === typeStr)

  def plugins[F[_]: MonadThrow]: F[Plugins] =
    MonadThrow[F].pure(
      NonEmptySeq.of(
        MultipleChoiceComponent(),
        TextComponent()
      )
    )

}

package component {

  final case class MultipleChoiceComponent()
      extends Component[field.MultipleChoice, answer.MultipleChoice] {

    def typeStr: String = "multiple-choice"

    implicit def decoderForField: Decoder[field.MultipleChoice] = ???

    implicit def encoderForField: Encoder[field.MultipleChoice] = ???

    implicit def decoderForAnswer: Decoder[answer.MultipleChoice] = ???

    implicit def encoderForAnswer: Encoder[answer.MultipleChoice] = ???

    def validate(
        aField: field.MultipleChoice,
        anAnswer: answer.MultipleChoice
    ): ValidatedNec[Throwable, answer.MultipleChoice] = anAnswer.validNec

  }

  final case class TextComponent() extends Component[field.Text, answer.Text] {

    def typeStr: String = "text"

    implicit def decoderForField: Decoder[field.Text] =
      new Decoder[field.Text] {
        def apply(c: HCursor): Decoder.Result[field.Text] = Right(field.Text())
      }

    implicit def encoderForField: Encoder[field.Text] =
      new Encoder[field.Text] {
        def apply(a: field.Text): Json = Json.obj()
      }

    implicit def decoderForAnswer: Decoder[answer.Text] =
      new Decoder[answer.Text] {
        def apply(c: HCursor): Decoder.Result[answer.Text] =
          for {
            value <- c.downField("value").as[String]
          } yield answer.Text(value)
      }

    implicit def encoderForAnswer: Encoder[answer.Text] =
      new Encoder[answer.Text] {
        def apply(a: answer.Text): Json = Json.obj(
          ("value", Json.fromString(a.value))
        )
      }

    def validate(
        aField: field.Text,
        anAnswer: answer.Text
    ): ValidatedNec[Throwable, answer.Text] = anAnswer.validNec
  }

}

package field {

  final case class MultipleChoice(
      choices: NonEmptySet[String],
      maxChoices: Int
  ) extends Field

  object MultipleChoice {

    def apply(
        choices: NonEmptySet[String],
        maxChoices: Int
    ): ValidatedNec[Throwable, MultipleChoice] =
      MultipleChoice(choices, maxChoices)

  }

  final case class Text() extends Field

}

package answer {

  final case class MultipleChoice(
      choices: NonEmptySet[String]
  ) extends Answer

  final case class Text(value: String) extends Answer

}
