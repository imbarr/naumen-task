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
    val add = phones.map(p => (p.name, p.phone)) += (entry.name, entry.phone)
    val getResult = GetResult(r => r.nextInt())
    val identity = sql"select @@IDENTITY".as[Int](getResult).head
    database.run(add.andThen(identity).withPinnedSession)
  }

  override def get(nameSubstring: Option[String],
                   phoneSubstring: Option[String],
                   range: Option[(Int, Int)]): Future[Seq[BookEntryWithId]] = {
    val filtered = containing(nameSubstring, phoneSubstring)
    val cropped = range match {
      case Some((start, end)) =>
        require(start >= 0 && end >= 0 && start <= end)
        filtered.drop(start).take(end - start + 1)
      case None =>
        filtered
    }
    database.run(cropped.result).map(toEntries)
  }

  override def getSize(nameSubstring: Option[String],
                        phoneSubstring: Option[String]): Future[Int] = {
    val query = containing(nameSubstring, phoneSubstring).length
    database.run(query.result)
  }

  override def getById(id: Int): Future[Option[BookEntry]] = {
    val query = phones
      .filter(_.id === id)
      .map(t => (t.name, t.phone))
      .result
      .headOption
    database.run(query).map(_.map(BookEntry.tupled))
  }

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

  private def containing(nameSubstring: Option[String],
                         phoneSubstring: Option[String]) = {
    val nameFiltered = nameSubstring match {
      case Some(substring) => phones.filter(p => charindex(substring, p.name) > 0)
      case None => phones
    }
    phoneSubstring match {
      case Some(substring) => nameFiltered.filter(p => charindex(substring, p.phone) > 0)
      case None => nameFiltered
    }
  }

  private def toEntries(tuples: Seq[(Int, String, String)]): Seq[BookEntryWithId] =
    tuples.map(BookEntryWithId.tupled)
}
