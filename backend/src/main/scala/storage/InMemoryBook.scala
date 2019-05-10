package storage
import data.{BookEntry, BookEntryWithId}

class InMemoryBook {
  private var map = Map[Int, BookEntry]()
  private var lastId = 0

  def add(entry: BookEntry): Int = {
    lastId += 1
    map += lastId -> entry
    lastId
  }

  def getAll: List[BookEntryWithId] =
    map.map(join).toList

  def changePhoneNumber(id: Int, phoneNumber: String): Boolean =
    change(id, entry => BookEntry(entry.name, phoneNumber))

  def changeName(id: Int, name: String): Boolean =
    change(id, entry => BookEntry(name, entry.phoneNumber))

  def replace(id: Int, name: String, phoneNumber: String): Boolean =
    change(id, _ => BookEntry(name, phoneNumber))

  private def change(id: Int, modification: BookEntry => BookEntry): Boolean =
    map.get(id) match {
      case None =>
        false
      case Some(entry) =>
        map += id -> modification(entry)
        true
    }

  def remove(id: Int) =
    map.get(id) match {
      case None =>
        false
      case Some(_) =>
        map -= id
        true
    }

  def findByNameSubstring(substring: String): List[BookEntryWithId] =
    map.filter{_._2.name.contains(substring)}.map(join).toList

  def findByPhoneNumberSubstring(substring: String): List[BookEntryWithId] =
    map.filter(_._2.phoneNumber.contains(substring)).map(join).toList

  private def join(pair: (Int, BookEntry)): BookEntryWithId =
    BookEntryWithId(pair._1, pair._2.name, pair._2.phoneNumber)
}
