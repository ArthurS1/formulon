package fr.konexii.formulon.application

sealed trait Role
sealed case class Admin() extends Role
sealed case class Org(orgName: String, identifier: String) extends Role
