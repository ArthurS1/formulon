package fr.konexii.formulon.domain

trait Field extends Named

final case class FieldWithMetadata(
    title: String,
    required: Boolean,
    field: Field
)

