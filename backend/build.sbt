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

  "com.github.pureconfig" %% "pureconfig" % "0.10.1",

  "org.slf4j" % "slf4j-simple" % "1.6.4",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
)