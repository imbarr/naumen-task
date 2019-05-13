package storage.tables

import slick.jdbc.SQLServerProfile.api._

class PhoneNumbers(tag: Tag) extends Table[(Int, String, String)](tag, "PhoneNumbers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  def phone = column[String]("phone")

  def * = (id, name, phone)
}
