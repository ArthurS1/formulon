/*
 * GLOBAL OPTIONS
 */

val http4sVersion = "0.23.30"
val circeVersion = "0.14.10"
val log4catsVersion = "2.7.0"
val skunkVersion = "0.6.4"
// This version is needed because log4cats depends on this slf4j-api version.
// If we update it, it brakes assembly.
val slf4jSimpleVersion = "1.7.36"

val compilerOptions = Seq(
  "-encoding", "utf8",
  "-Xlint",
  "-Wunused:_",
  "-Wdead-code",
  "-Wnumeric-widen",
  "-Wunnamed-boolean-literal",
  "-Wnonunit-statement",
  "-Wnonunit-if",
  "-Wextra-implicit",
  "-Wvalue-discard",
  "-Wmacros:after",
  "-feature",
  //"-Werror", // Makes the LSP explode in neovim without the MetaLS extension
)

val testCompilerOptions = compilerOptions ++ Seq(
  "-Wconf:msg=unused value of type org.scalatest.Assertion:s",
)

/*
 * CORE
 */

lazy val core = project
  .in(file("core"))
  .settings(
    name := "Core",
    moduleName := "formulon-core",
    description := "Contains the domain of the project. Basis for the server and for the scalaJS client part.",
    scalaVersion := "2.13.16",
    organization := "fr.konexii",
    Compile / scalacOptions := compilerOptions,
    Test / scalacOptions := testCompilerOptions,
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.7",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "org.typelevel" %% "cats-laws" % "2.13.0" % Test,
    libraryDependencies += "org.typelevel" %% "discipline-scalatest" % "2.3.0" % Test
  )

/*
 * PLUGIN
 */

lazy val plugins = project
  .in(file("plugin"))
  .settings(
    name := "Plugin",
    moduleName := "formulon-plugin",
    description := "Contains the plugin interface. Needed when you want to create a new plugin.",
    scalaVersion := "2.13.16",
    organization := "fr.konexii",
    scalacOptions := compilerOptions,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
  )
  .dependsOn(core)

/*
 * SERVER
 */

lazy val server = project
  .in(file("server"))
  .settings(
    name := "Server",
    moduleName := "formulon-server",
    description := "Http server for creating schemas and filling up forms.",
    scalaVersion := "2.13.16",
    organization := "fr.konexii",
    Compile / scalacOptions := compilerOptions,
    Test / scalacOptions := testCompilerOptions,
    assembly / assemblyJarName := s"formulon-${version.value}.jar",
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
    // Postgres database access
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "skunk-core",
      "org.tpolecat" %% "skunk-circe"
    ).map(_ % skunkVersion),
    // JWTs
    libraryDependencies += "com.github.jwt-scala" %% "jwt-circe" % "10.0.4",
    // Logging
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
    libraryDependencies += "org.typelevel" %% "log4cats-noop" % log4catsVersion % Test,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jSimpleVersion,
    // Tracing (necessary for skunk at this point, should be replaced by otel)
    libraryDependencies += "org.tpolecat" %% "natchez-core" % "0.3.7",
    // Testing
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  )
  .dependsOn(plugins)
