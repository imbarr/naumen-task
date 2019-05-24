import java.io.Closeable

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.Config
import config.Readers._
import pureconfig.generic.auto._
import slick.jdbc.SQLServerProfile.api.Database
import storage.{Book, DatabaseBook, EntriesCleaner}

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

  private def startServer(config: Config): Unit = {
    log.info("Starting server...")
    val database = Database.forURL(
      config.database.databaseUrl,
      config.database.backend.user,
      config.database.backend.password
    )
    val book = new DatabaseBook(database, getEntryLifespan(config))
    val cleanerOption = getCleaner(database, config)
    val bindingFuture = getServer(book, config).start(config.server)
    log.info(s"Server is up on ${config.server.interface}:${config.server.port}. Press ENTER to quit.")
    StdIn.readLine()
    exit(bindingFuture, Seq(database) ++ cleanerOption)
  }

  private def getCleaner(database: Database, config: Config): Option[EntriesCleaner] =
    for {
      lifespan <- getEntryLifespan(config)
      interval <-  getCleanupInterval(config)
    } yield new EntriesCleaner(database, lifespan, interval)

  private def getServer(book: Book, config: Config): Server = {
    val dataSaver = new DataSaver(config.fileSystem.storage)
    val taskManager = new TaskManager()
    new Server(book, dataSaver, taskManager)
  }

  private def getEntryLifespan(config: Config): Option[Long] =
    config.database.lifespan.map(_.entryLifespan.millis)

  private def getCleanupInterval(config: Config): Option[Long] =
    config.database.lifespan.flatMap(_.cleanupInterval).map(_.millis)

  private def exit(bindingFuture: Future[ServerBinding], closeables: Seq[Closeable]): Unit = {
    val future = for {
      binding <- bindingFuture
      _ <- binding.unbind()
      _ <- Future {
        for (closeable <- closeables.par)
          closeable.close()
      }
    } yield Unit
    future.onComplete(_ => system.terminate())
  }
}
