import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.{Config, DatabaseConfig}
import pureconfig.generic.auto._
import storage.InMemoryBook

import scala.io.StdIn

object Main extends App {
  implicit val log = Logger("naumen-task")
  implicit val system = ActorSystem("naumen-task")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext


  pureconfig.loadConfig[Config] match {
    case Left(_) =>
      log.error("FATAL: failed to load configuration")
      System.exit(1)
    case Right(config) =>
      migrate(config.database)
      val book = new InMemoryBook()
      log.info("Starting server...")
      val bindingFuture = new Server(book).start(config.server)
      log.info(s"Server is up on ${config.server.interface}:${config.server.port}. Press ENTER to quit.")
      bindingFuture.flatMap(binding => {
        StdIn.readLine()
        binding.unbind()
      }).onComplete(_ => system.terminate())
  }

  def migrate(config: DatabaseConfig): Unit = {
    config.migration match {
      case None =>
        log.warn("Credentials for database migrations were not specified. Migrations will not run")
      case Some(credentials) =>
        new Migration(config.url, credentials).migrate() match {
          case Some(error) =>
            log.error("Error occurred while performing migrations", error)
          case None =>
            log.info("Database migration successful")
        }
    }
  }
}
