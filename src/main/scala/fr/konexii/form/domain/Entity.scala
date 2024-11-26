package fr.konexii.form
package domain

import java.util.UUID

case class Entity[T](id: UUID, data: T)
