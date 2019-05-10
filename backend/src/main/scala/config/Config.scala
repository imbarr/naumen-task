package config

case class Config(server: ServerConfig)

case class ServerConfig(interface: String, port: Int)