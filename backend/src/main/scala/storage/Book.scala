package storage

import data.{BookEntry, BookEntryWithId}

import scala.concurrent.Future

trait Book {
  def add(entry: BookEntry): Future[Int]

  def getAll: Future[Seq[BookEntryWithId]]

  def getSize: Future[Int]

  def getRange(start: Int, end: Int): Future[Seq[BookEntryWithId]]

  def changePhoneNumber(id: Int, phoneNumber: String): Future[Boolean]

  def changeName(id: Int, name: String): Future[Boolean]

  def replace(id: Int, name: String, phoneNumber: String): Future[Boolean]

  def remove(id: Int): Future[Boolean]

  def findByNameSubstring(substring: String): Future[Seq[BookEntryWithId]]

  def findByPhoneNumberSubstring(substring: String): Future[Seq[BookEntryWithId]]
}