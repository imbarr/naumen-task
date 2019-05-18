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
    val nameFiltered = nameSubstring match {
      case Some(substring) => map.filter(_._2.name.contains(substring))
      case None => map
    }
    val phoneFiltered = phoneSubstring match {
      case Some(substring) => nameFiltered.filter(_._2.phoneNumber.contains(substring))
      case None => nameFiltered
    }
    val cropped = range match {
      case Some((start, end)) =>
        require(start >= 0 && end >= 0 && start <= end)
        phoneFiltered.slice(start, end + 1)
      case None =>
        phoneFiltered
    }
    val result = cropped.map(join).toList
    Future.successful(result)
  }

  override def getSize: Future[Int] =
    Future.successful(map.size)

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
