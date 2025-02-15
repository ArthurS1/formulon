val http4sVersion = "0.23.29"
val circeVersion = "0.14.10"
val log4catsVersion = "2.7.0"

/*
 * I have to be honest here, everything that happens in this file is dark magic to me.
 * Any review is appreciated.
 */

lazy val formulon = project
  .in(file("."))
  .settings(
    name := "Formulon",
    version := "0.1",
    scalaVersion := "2.13.15",
    organization := "fr.konexii",
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      //"-feature",
      //"-unchecked",
      //"-Xlint",
      //"-Xfatal-warnings",
      //"-Ywarn-dead-code",
      //"-Ywarn-numeric-widen",
      //"-Ywarn-value-discard"
      ),
    assemblyMergeStrategy := {
      case PathList("META-INF", "versions", _*) => MergeStrategy.first
      case x => {
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
      }
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
    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    // Logging
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.16",
    // Tracing
    libraryDependencies += "org.tpolecat" %% "natchez-core" % "0.3.5",
    // Postgres database access
    libraryDependencies += "org.tpolecat" %% "skunk-core" % "0.6.4",
    // Inserting JSONs in postgres
    libraryDependencies += "org.tpolecat" %% "skunk-circe" % "0.6.4",
  )
