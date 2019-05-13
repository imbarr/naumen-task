package config

case class Config(server: ServerConfig, database: DatabaseConfig)

case class ServerConfig(interface: String, port: Int)

case class DatabaseConfig(url: String, backend: Credentials, migration: Credentials)

case class Credentials(user: String, password: String)