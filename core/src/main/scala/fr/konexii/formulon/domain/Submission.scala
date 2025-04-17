package fr.konexii.formulon.domain

trait Answer extends Named

final case class Submission(
    answers: List[Entity[Answer]]
)

