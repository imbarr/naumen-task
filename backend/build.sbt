scalaVersion := "2.12.8"

name := "naumen-task"

val circeVersion = "0.10.0"
val akkaHttpVersion = "10.1.5"
val akkaVersion = "2.5.18"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-caching" % akkaHttpVersion,

  "com.github.pureconfig" %% "pureconfig" % "0.10.1",

  "com.typesafe.slick" %% "slick" % "3.3.0",

  "org.slf4j" % "slf4j-simple" % "1.6.4",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  "org.scalamock" %% "scalamock" % "4.1.0" % Test,

  "org.flywaydb" % "flyway-core" % "5.2.4",

  "com.microsoft.sqlserver" % "mssql-jdbc" % "7.0.0.jre10",

  "com.google.jimfs" % "jimfs" % "1.1",

  "com.googlecode.libphonenumber" % "libphonenumber" % "8.10.12"
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.15")