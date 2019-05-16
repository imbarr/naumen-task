package storage

import data.{BookEntry, BookEntryWithId}

import scala.concurrent.Future

class InMemoryBook extends Book {
  private var map = Map[Int, BookEntry]()
  private var lastId = 0

  override def add(entry: BookEntry): Future[Int] = {
    lastId += 1
    map += lastId -> entry
    Future.successful(lastId)
  }

  override def getAll: Future[List[BookEntryWithId]] = {
    val result = map.map(join).toList
    Future.successful(result)
  }

  def getRange(start: Int, end: Int): Future[List[BookEntryWithId]] = {
    require(start >= 0 && end >= 0 && start <= end)
    val result = map.slice(start, end + 1).map(join).toList
    Future.successful(result)
  }

  override def changePhoneNumber(id: Int, phoneNumber: String): Future[Boolean] =
    change(id, entry => BookEntry(entry.name, phoneNumber))

  override def changeName(id: Int, name: String): Future[Boolean] =
    change(id, entry => BookEntry(name, entry.phoneNumber))

  override def replace(id: Int, name: String, phoneNumber: String): Future[Boolean] =
    change(id, _ => BookEntry(name, phoneNumber))

  override def remove(id: Int): Future[Boolean] =
    map.get(id) match {
      case None =>
        Future.successful(false)
      case Some(_) =>
        map -= id
        Future.successful(true)
    }

  override def findByNameSubstring(substring: String): Future[List[BookEntryWithId]] = {
    val filtered = map.filter(_._2.name.contains(substring))
    val result = filtered.map(join).toList
    Future.successful(result)
  }

  override def findByPhoneNumberSubstring(substring: String): Future[List[BookEntryWithId]] = {
    val filtered = map.filter(_._2.phoneNumber.contains(substring))
    val result = filtered.map(join).toList
    Future.successful(result)
  }

  private def change(id: Int, modification: BookEntry => BookEntry): Future[Boolean] =
    map.get(id) match {
      case None =>
        Future.successful(false)
      case Some(entry) =>
        map += id -> modification(entry)
        Future.successful(true)
    }

  private def join(pair: (Int, BookEntry)): BookEntryWithId =
    BookEntryWithId(pair._1, pair._2.name, pair._2.phoneNumber)
}
