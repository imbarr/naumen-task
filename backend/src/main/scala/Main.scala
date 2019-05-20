import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.{Config, DatabaseConfig, ServerConfig}
import pureconfig.generic.auto._
import slick.jdbc.SQLServerProfile.api.Database
import storage.{Book, DatabaseBook}

import scala.io.StdIn

object Main extends App {
  implicit val log = Logger("naumen-task")
  implicit val system = ActorSystem("naumen-task")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  pureconfig.loadConfig[Config] match {
    case Left(failures) =>
      log.error("FATAL: failed to load configuration: " + failures)
      System.exit(1)
    case Right(config) =>
      Migration.migrate(config.database)
      val book = getDatabaseBook(config.database)
      val directoryPath = Paths.get(config.fileSystem.storage)
      val dataSaver = new DataSaver(directoryPath)
      startServer(config.server, book, dataSaver)
  }

  def getDatabaseBook(config: DatabaseConfig): DatabaseBook = {
    val database = Database.forURL(config.databaseUrl, config.backend.user, config.backend.password)
    new DatabaseBook(database)
  }

  def startServer(config: ServerConfig, book: Book, dataSaver: DataSaver): Unit = {
    val taskManager = new TaskManager()
    log.info("Starting server...")
    val bindingFuture = new Server(book, dataSaver, taskManager).start(config)
    log.info(s"Server is up on ${config.interface}:${config.port}. Press ENTER to quit.")
    bindingFuture.flatMap(binding => {
      StdIn.readLine()
      binding.unbind()
    }).onComplete(_ => system.terminate())
  }
}
