import com.typesafe.scalalogging.Logger
import config.Config
import pureconfig.generic.auto._
import slick.jdbc.SQLServerProfile.api._
import storage.tables.PhoneNumbers
import storage.{Book, DatabaseBook}

import scala.concurrent.ExecutionContext

class DatabaseBookSpec extends BookBehaviours {
  implicit val executionContext = ExecutionContext.global
  implicit val log = Logger("naumen-task-test")

  val config = pureconfig
    .loadConfig[Config]
    .fold(_ => throw new RuntimeException("Failed to load config"), identity)
    .database

  Migration.migrate(config)
  val database = Database.forURL(config.databaseUrl, config.backend.user, config.backend.password)
  override var book: Book = new DatabaseBook(database)

  after {
    def havingSameIds(entry: PhoneNumbers): Rep[Boolean] =
      added.map(entry.id === _.id).reduceLeft(_ || _)

    val query = TableQuery[PhoneNumbers].filter(havingSameIds).delete
    await(database.run(query))
  }

  "DatabaseBook" should behave like anyBook
}
