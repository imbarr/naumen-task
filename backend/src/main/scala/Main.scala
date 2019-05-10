import com.typesafe.scalalogging.Logger
import config.Config
import storage.InMemoryBook
import pureconfig.generic.auto._

object Main extends App {
  implicit val log = Logger("naumen-task")

  pureconfig.loadConfig[Config] match {
    case Left(_) =>
      log.error("FATAL: failed to load configuration")
      System.exit(1)
    case Right(config) =>
      val book = new InMemoryBook()
      new Server(book).startServer(config.server.interface, config.server.port)
  }
}
