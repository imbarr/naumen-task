import java.io.Closeable
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.{Config, DatabaseConfig, ServerConfig}
import pureconfig.generic.auto._
import slick.jdbc.SQLServerProfile.api.Database
import storage.{Book, DatabaseBook}

import scala.concurrent.Future
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
      startServer(config)
  }

  def startServer(config: Config): Unit = {
    log.info("Starting server...")
    val database = Database.forURL(
      config.database.databaseUrl,
      config.database.backend.user,
      config.database.backend.password
    )
    val book = new DatabaseBook(database)
    val directoryPath = Paths.get(config.fileSystem.storage)
    val dataSaver = new DataSaver(directoryPath)
    val taskManager = new TaskManager()
    val bindingFuture = new Server(book, dataSaver, taskManager).start(config.server)
    log.info(s"Server is up on ${config.server.interface}:${config.server.port}. Press ENTER to quit.")
    StdIn.readLine()
    exit(bindingFuture, database, book)
  }

  private def exit(bindingFuture: Future[ServerBinding], closeables: Closeable*): Unit = {
    val future = for {
      binding <- bindingFuture
      _ <- binding.unbind()
      _ <- Future {
        for (closeable <- closeables.par)
          closeable.close()
      }
    } yield Unit
    future.onComplete(_ -> system.terminate())
  }
}
