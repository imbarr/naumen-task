package config

import java.nio.file.Paths

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

import scala.concurrent.duration.Duration
import scala.util.Try

object Readers {
  implicit val pathReader = functionToConfigReader(Paths.get(_), "java.nio.file.Path")

  implicit val millisFromDurationReader =
    functionToConfigReader(durationStringToMillis, "java.time.Duration")

  implicit val scalaDurationReader =
    functionToConfigReader(stringToPositiveDuration, "scala.concurrent.duration.Duration")

  private def functionToConfigReader[T](function: String => T, typeName: String): ConfigReader[T] =
    ConfigReader.fromString { string =>
      Try(function(string)).toEither.left.map(error => CannotConvert(string, typeName, error.getMessage))
    }

  private def durationStringToMillis(string: String): Milliseconds = {
    val duration = java.time.Duration.parse(string)
    Milliseconds(duration.toMillis)
  }

  private def stringToPositiveDuration(string: String): Duration = {
    val duration = Duration(string)
    require(duration > Duration.Zero)
    duration
  }
}