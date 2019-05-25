import java.time.LocalDateTime

import com.typesafe.scalalogging.Logger
import config.Config
import config.Readers._
import data.BookEntry
import org.scalatest.BeforeAndAfterAll
import pureconfig.generic.auto._
import slick.jdbc.SQLServerProfile.api._
import storage.database.tables.PhoneNumbers
import storage.{Book, DatabaseBook, EntriesCleaner}
import util.TestUtils._

import scala.concurrent.ExecutionContext

class DatabaseSpec extends BookBehaviours with BeforeAndAfterAll {
  implicit val log = Logger("naumen-task-test")
  implicit val executionContext = ExecutionContext.global

  val config = pureconfig
    .loadConfig[Config]
    .fold(_ => throw new RuntimeException("Failed to load config"), identity)
    .database

  Migration.migrate(config)

  val database = Database.forURL(config.databaseUrl, config.backend.user, config.backend.password)

  override var book: Book = new DatabaseBook(database)
  var cleaner: EntriesCleaner = _

  after {
    for (entry <- added) {
      val query = TableQuery[PhoneNumbers].filter(_.id === entry.id).delete
      await(database.run(query))
    }
  }

  "DatabaseBook" should behave like anyBook

  it should "return only unexpired entries" in {
    val lifespan = 500
    val book = new DatabaseBook(database, Some(lifespan))
    val old = await(book.get()).toSet
    Thread.sleep(lifespan)
    val fresh = addEntries(anotherEntries).toSet
    added ++= fresh
    Thread.sleep(lifespan / 3)
    val result = await(book.get()).toSet
    assert(fresh subsetOf result)
    assert((old intersect result) == Set())
  }

  "EntriesCleaner" should "delete only expired entries" in {
    val hundredYearsInMillis = 100 * 365 * 24 * 60 * 60 * 1000
    val interval = 500
    val before = await(book.get())
    cleaner = new EntriesCleaner(database, hundredYearsInMillis, interval)
    addWithDatetime(anotherEntries, LocalDateTime.now().minusYears(101))
    Thread.sleep(interval + 100)
    val after = await(book.get())
    assert(before.toSet == after.toSet)
  }

  override def afterAll(): Unit = {
    cleaner.close()
    database.close()
  }

  private def addWithDatetime(entries: Seq[BookEntry], datetime: LocalDateTime): Unit = {
    val query = TableQuery[PhoneNumbers].map(p => (p.name, p.phone, p.created)) ++=
      entries.map(entry => (entry.name, entry.phone.withoutDelimiters, datetime))
    await(database.run(query))
  }
}
