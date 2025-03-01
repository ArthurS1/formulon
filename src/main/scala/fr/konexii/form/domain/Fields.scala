package fr.konexii.form.domain.field

final case class FieldWithMetadata(
    title: String,
    required: Boolean,
    field: Field
)

trait Field {
  def typeStr: String
}

