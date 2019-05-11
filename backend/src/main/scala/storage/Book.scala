package storage

import data.{BookEntry, BookEntryWithId}

trait Book {
  def add(entry: BookEntry): Int

  def getAll: Seq[BookEntryWithId]

  def changePhoneNumber(id: Int, phoneNumber: String): Boolean

  def changeName(id: Int, name: String): Boolean

  def replace(id: Int, name: String, phoneNumber: String): Boolean

  def remove(id: Int): Boolean

  def findByNameSubstring(substring: String): Seq[BookEntryWithId]

  def findByPhoneNumberSubstring(substring: String): Seq[BookEntryWithId]
}