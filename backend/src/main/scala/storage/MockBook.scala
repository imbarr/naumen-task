package storage
import data.BookEntry

class MockBook extends Book {
  override def add(entry: BookEntry) = ???

  override def getAll = ???

  override def changePhoneNumber(id: Int, phoneNumber: String) = ???

  override def changeName(id: Int, name: String) = ???

  override def replace(id: Int, name: String, phoneNumber: String) = ???

  override def remove(id: Int) = ???

  override def findByNameSubstring(substring: String) = ???

  override def findByPhoneNumberSubstring(substring: String) = ???
}
