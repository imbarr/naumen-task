package config

import java.nio.file.Path

import scala.concurrent.duration.Duration

case class Config(server: ServerConfig, database: DatabaseConfig, fileSystem: FileSystemConfig)

case class ServerConfig(interface: String, port: Int, cache: Option[CacheConfig])

case class DatabaseConfig(url: String,
                          database: String,
                          lifespan: Option[LifespanConfig],
                          backend: Credentials,
                          migration: Option[Credentials]) {
  val databaseUrl = s"$url;databaseName=$database"
}

case class FileSystemConfig(storage: Path)

case class CacheConfig(maxCapacity: Int, initialCapacity: Int, timeToLive: Duration, timeToIdle: Duration)

case class LifespanConfig(entryLifespan: Milliseconds, cleanupInterval: Option[Milliseconds] = None)

case class Credentials(user: String, password: String)

case class Milliseconds(millis: Long)