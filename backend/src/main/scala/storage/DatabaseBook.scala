package storage
import data.{BookEntry, BookEntryWithId}
import slick.jdbc.GetResult
import slick.jdbc.SQLServerProfile.api._
import storage.tables.PhoneNumbers

import scala.concurrent.{ExecutionContext, Future}

class DatabaseBook(database: Database)(implicit context: ExecutionContext) extends Book {
  private val phones = TableQuery[PhoneNumbers]
  private val charindex = SimpleFunction.binary[String, String, Int]("charindex")

  override def add(entry: BookEntry): Future[Int] = {
    val add = phones.map(p => (p.name, p.phone)) += (entry.name, entry.phoneNumber)
    val getResult = GetResult(r => r.nextInt())
    val identity = sql"select @@IDENTITY".as[Int](getResult).head
    database.run(add.andThen(identity).withPinnedSession)
  }

  override def getAll: Future[Seq[BookEntryWithId]] =
    database.run(phones.result).map(toEntries)

  override def changePhoneNumber(id: Int, phoneNumber: String): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(_.phone).update(phoneNumber)
    database.run(query).map(_ == 1)
  }

  override def changeName(id: Int, name: String): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(_.name).update(name)
    database.run(query).map(_ == 1)
  }

  override def replace(id: Int, name: String, phoneNumber: String): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(p => (p.name, p.phone)).update((name, phoneNumber))
    database.run(query).map(_ == 1)
  }

  override def remove(id: Int): Future[Boolean] = {
    val query = phones.filter(_.id === id).delete
    database.run(query).map(_ == 1)
  }

  override def findByNameSubstring(substring: String): Future[Seq[BookEntryWithId]] = {
    val query = phones.filter(p => charindex(substring, p.name) > 0).result
    database.run(query).map(toEntries)
  }

  override def findByPhoneNumberSubstring(substring: String): Future[Seq[BookEntryWithId]] = {
    val query = phones.filter(p => charindex(substring, p.phone) > 0).result
    database.run(query).map(toEntries)
  }

  private def toEntries(tuples: Seq[(Int, String, String)]): Seq[BookEntryWithId] =
    tuples.map(BookEntryWithId.tupled)
}
