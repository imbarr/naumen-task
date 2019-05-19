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

  override def get(nameSubstring: Option[String],
                   phoneSubstring: Option[String],
                   range: Option[(Int, Int)]): Future[Seq[BookEntryWithId]] = {
    val filtered = containing(nameSubstring, phoneSubstring)
    val cropped = range match {
      case Some((start, end)) =>
        require(start >= 0 && end >= 0 && start <= end)
        filtered.slice(start, end + 1)
      case None =>
        filtered
    }
    val result = cropped.map(join).toList
    Future.successful(result)
  }

  override def getSize(nameSubstring: Option[String],
                       phoneSubstring: Option[String]): Future[Int] = {
    val result = containing(nameSubstring, phoneSubstring).size
    Future.successful(result)
  }

  override def getById(id: Int): Future[Option[BookEntry]] =
    Future.successful(map.get(id))

  override def changePhoneNumber(id: Int, phoneNumber: String): Future[Boolean] =
    change(id, entry => BookEntry(entry.name, phoneNumber))

  override def changeName(id: Int, name: String): Future[Boolean] =
    change(id, entry => BookEntry(name, entry.phone))

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

  private def containing(nameSubstring: Option[String],
                         phoneSubstring: Option[String]): Map[Int, BookEntry] = {
    val nameFiltered = nameSubstring match {
      case Some(substring) => map.filter(_._2.name.contains(substring))
      case None => map
    }
    phoneSubstring match {
      case Some(substring) => nameFiltered.filter(_._2.phone.contains(substring))
      case None => nameFiltered
    }
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
    BookEntryWithId(pair._1, pair._2.name, pair._2.phone)
}
