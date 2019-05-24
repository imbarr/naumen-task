package storage.database.tables

import java.time.LocalDateTime

import slick.jdbc.SQLServerProfile.api._

class PhoneNumbers(tag: Tag) extends Table[(Int, String, String, LocalDateTime)](tag, "PhoneNumbers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  def phone = column[String]("phone")

  def created = column[LocalDateTime]("created")

  def * = (id, name, phone, created)


}
