package fr.konexii.form.domain.answer

import fr.konexii.form.domain._

/*
 * This structure is created with the idea that in the future we might want to
 * attach more data to a the client's list of answers (like timestamp or custom data).
 */
final case class Submission(
    answers: List[Entity[Answer]]
)

trait Answer {
  def typeStr: String
}
