package storage

import data.{BookEntry, BookEntryWithId, Phone}

import scala.concurrent.Future

trait Book {
  def add(entry: BookEntry): Future[Option[Int]]

  def getEntries(nameSubstring: Option[String] = None,
                 phoneSubstring: Option[String] = None,
                 range: Option[(Int, Int)] = None): Future[Seq[BookEntryWithId]]

  def getSize(nameSubstring: Option[String] = None,
              phoneSubstring: Option[String] = None): Future[Int]

  def getById(id: Int): Future[Option[BookEntry]]

  def changePhoneNumber(id: Int, phone: Phone): Future[Boolean]

  def changeName(id: Int, name: String): Future[Boolean]

  def replace(id: Int, name: String, phone: Phone): Future[Boolean]

  def remove(id: Int): Future[Boolean]
}