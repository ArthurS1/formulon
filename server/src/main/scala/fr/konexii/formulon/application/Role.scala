package fr.konexii.formulon.application

sealed trait Role
sealed case class Admin() extends Role
sealed case class Org(name: String, email: String) extends Role
