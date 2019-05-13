import config.Credentials
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException

class Migration(url: String, credentials: Credentials) {
  val flyway = Flyway.configure().dataSource(url, credentials.user, credentials.password).load()

  def migrate(): Option[FlywayException] =
    try {
      flyway.migrate()
      None
    } catch {
      case exception: FlywayException =>
        Some(exception)
    }
}
