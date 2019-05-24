package config

import java.nio.file.Path

case class Config(server: ServerConfig, database: DatabaseConfig, fileSystem: FileSystemConfig)

case class ServerConfig(interface: String, port: Int)

case class DatabaseConfig(url: String,
                          database: String,
                          lifespan: Option[LifespanConfig],
                          backend: Credentials,
                          migration: Option[Credentials]) {
  val databaseUrl = s"$url;databaseName=$database"
}

case class FileSystemConfig(storage: Path)

case class Credentials(user: String, password: String)

case class LifespanConfig(entryLifespan: Milliseconds, cleanupInterval: Option[Milliseconds] = None)

case class Milliseconds(millis: Long)