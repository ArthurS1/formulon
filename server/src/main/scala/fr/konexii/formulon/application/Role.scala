package fr.konexii.formulon.application

sealed trait Role
sealed case class Admin() extends Role
sealed case class Editor(orgName: String, identifier: String) extends Role
