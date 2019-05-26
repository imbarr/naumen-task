package storage.database

import java.time.LocalDateTime

import com.microsoft.sqlserver.jdbc.SQLServerException
import data.{BookEntry, BookEntryWithId, Phone}
import slick.jdbc.GetResult
import slick.jdbc.SQLServerProfile.api._
import storage.Book
import storage.database.Functions._
import storage.database.tables.PhoneNumbers

import scala.concurrent.{ExecutionContext, Future}

class DatabaseBook(database: Database, lifespanInMillis: Option[Long] = None)
                  (implicit context: ExecutionContext) extends Book {

  private val phones = TableQuery[PhoneNumbers]

  override def add(entry: BookEntry): Future[Option[Int]] = {
    val add = phones.map(p => (p.name, p.phone)) += (entry.name, entry.phone.withoutDelimiters)
    val getResult = GetResult(r => r.nextInt())
    val getIdentity = sql"select @@IDENTITY".as[Int](getResult).head
    database.run(add.andThen(getIdentity).withPinnedSession).map(Some.apply).recover {
      case exception: SQLServerException if isUniqueConstraintViolation(exception) =>
        None
    }
  }

  override def getEntries(nameSubstring: Option[String],
                          phoneSubstring: Option[String],
                          range: Option[(Int, Int)]): Future[Seq[BookEntryWithId]] = {
    val filtered = unexpiredAndContainingQuery(nameSubstring, phoneSubstring)
    val cropped = range match {
      case Some((start, end)) =>
        require(start >= 0 && end >= 0 && start <= end)
        filtered.drop(start).take(end - start + 1)
      case None =>
        filtered
    }
    for {
      tuples <- database.run(cropped.result)
    } yield tuples.flatMap(toBookEntryWithId)
  }

  override def getSize(nameSubstring: Option[String],
                       phoneSubstring: Option[String]): Future[Int] = {
    val query = unexpiredAndContainingQuery(nameSubstring, phoneSubstring).length
    database.run(query.result)
  }

  override def getById(id: Int): Future[Option[BookEntry]] = {
    val query = phones
      .filter(_.id === id)
      .map(t => (t.name, t.phone))
      .result
      .headOption
    for {
      tupleOption <- database.run(query)
    } yield tupleOption.flatMap(toBookEntry)
  }

  override def changePhoneNumber(id: Int, phone: Phone): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(_.phone).update(phone.withoutDelimiters)
    database.run(query).map(_ == 1)
  }

  override def changeName(id: Int, name: String): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(_.name).update(name)
    database.run(query).map(_ == 1)
  }

  override def replace(id: Int, name: String, phone: Phone): Future[Boolean] = {
    val query = phones.filter(_.id === id).map(p => (p.name, p.phone)).update((name, phone.withoutDelimiters))
    database.run(query).map(_ == 1)
  }

  override def remove(id: Int): Future[Boolean] = {
    val query = phones.filter(_.id === id).delete
    database.run(query).map(_ == 1)
  }

  private def unexpiredAndContainingQuery(nameSubstring: Option[String],
                                          phoneSubstring: Option[String]) = {
    val unexpired = lifespanInMillis match {
      case Some(lifespan) => phones.filter(_.created > addMillis(-lifespan, now))
      case None => phones
    }
    val nameFiltered = nameSubstring match {
      case Some(substring) => unexpired.filter(p => indexOf(substring, p.name) > 0)
      case None => unexpired
    }
    phoneSubstring.map(Phone.withoutDelimiters) match {
      case Some(substring) => nameFiltered.filter(p => indexOf(substring, p.phone) > 0)
      case None => nameFiltered
    }
  }

  private def isUniqueConstraintViolation(exception: SQLServerException): Boolean = {
    val code = exception.getErrorCode
    code == 2627 || code == 2601
  }

  private def toBookEntryWithId(tuple: (Int, String, String, LocalDateTime)): Option[BookEntryWithId] = {
    tuple match {
      case (id, name, phoneString, created) =>
        for {
          phone <- Phone.fromString(phoneString).toOption
        } yield BookEntryWithId(id, name, phone)
    }
  }

  private def toBookEntry(tuple: (String, String)): Option[BookEntry] =
    tuple match {
      case (name, phoneString) =>
        for {
          phone <- Phone.fromString(phoneString).toOption
        } yield BookEntry(name, phone)
    }
}
