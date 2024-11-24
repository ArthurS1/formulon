val http4sVersion = "0.23.29"
val circeVersion = "0.14.10"
val log4catsVersion = "2.7.0"

lazy val formService = (project in file(".")).settings(
  name := "Formulon",
  version := "0.1",
  scalaVersion := "2.13.15",
  organization := "fr.konexii",
  assemblyMergeStrategy := {
    case PathList("META-INF", "versions", _*) => MergeStrategy.first
    case x => (assemblyMergeStrategy in assembly).value(x)
  },
  // Http4s specific configuration
  run / fork := true,
  // Http4s & ember
  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-circe"
  ).map(_ % http4sVersion),
  // JSON parsing and serialization
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
  // Pretty printing objects for debug & log
  libraryDependencies += "com.lihaoyi" %% "pprint" % "0.9.0",
  // Testing library
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  // Logging
  libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.16",
  // Database access
  libraryDependencies += "com.typesafe.slick" %% "slick" % "3.5.2",
  libraryDependencies += "org.postgresql" % "postgresql" % "42.7.4"
)


