package storage
import data.{BookEntry, BookEntryWithId}
import slick.jdbc.SQLServerProfile.api._
import storage.tables.PhoneNumbers

class DatabaseBook(database: Database) {
  private val phoneNumbers = TableQuery[PhoneNumbers]

  val charindex = SimpleFunction.binary[String, String, Int]("charindex")

  def add(entry: BookEntry) = database.run(phoneNumbers.map(p => (p.name, p.phone)) += (entry.name, entry.phoneNumber))

  def getAll = database.stream(phoneNumbers.result).mapResult(BookEntryWithId.tupled)

  def changePhoneNumber(id: Int, phoneNumber: String) =
    database.run(phoneNumbers.filter(_.id === id).map(_.phone).update(phoneNumber))

  def changeName(id: Int, name: String) =
    database.run(phoneNumbers.filter(_.id === id).map(_.name).update(name))

  def replace(id: Int, name: String, phoneNumber: String) =
    database.run(phoneNumbers.filter(_.id === id).map(p => (p.name, p.phone)).update((name, phoneNumber)))

  def remove(id: Int) = database.run(phoneNumbers.filter(_.id === id).delete)

  def findByNameSubstring(substring: String) =
    database.stream(phoneNumbers.filter(p => charindex(substring, p.name) > 0).result).mapResult(BookEntryWithId.tupled)

  def findByPhoneNumberSubstring(substring: String) =
    database.stream(phoneNumbers.filter(p => charindex(substring, p.phone) > 0).result).mapResult(BookEntryWithId.tupled)
}
