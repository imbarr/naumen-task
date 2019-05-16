package config

case class Config(server: ServerConfig, database: DatabaseConfig)

case class ServerConfig(interface: String, port: Int)

case class DatabaseConfig(url: String, database: String, backend: Credentials, migration: Option[Credentials]) {
  val databaseUrl = s"$url;databaseName=$database"
}

case class Credentials(user: String, password: String)