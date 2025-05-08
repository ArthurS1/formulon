package fr.konexii.formulon.domain

trait KeyedException {
  def key: String = this.getClass().getName()
}
