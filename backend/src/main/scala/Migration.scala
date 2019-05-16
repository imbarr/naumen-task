import com.typesafe.scalalogging.Logger
import config.DatabaseConfig
import org.flywaydb.core.Flyway

import scala.util.{Failure, Success, Try}

object Migration {
  def migrate(config: DatabaseConfig)(implicit log: Logger): Unit = {
    config.migration match {
      case None =>
        log.warn("Credentials for database migrations were not specified. Migrations will not run")
      case Some(credentials) =>
        val flyway = Flyway.configure().dataSource(config.databaseUrl, credentials.user, credentials.password).load()
        putPlaceholders(flyway, config)
        Try(flyway.migrate()) match {
          case Failure(error) =>
            log.error("Error occurred while performing migrations", error)
          case Success(_) =>
            log.info("Database migration successful")
        }
    }
  }

  private def putPlaceholders(flyway: Flyway, config: DatabaseConfig): Unit = {
    val placeholders = flyway.getConfiguration.getPlaceholders
    placeholders.put("user", config.backend.user)
    placeholders.put("password", config.backend.password)
    placeholders.put("database", config.database)
  }
}
