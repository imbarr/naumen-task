package storage.database

import java.time.LocalDateTime

import slick.jdbc.SQLServerProfile.api._

object Functions {
  val indexOf = SimpleFunction.binary[String, String, Int]("charindex")

  val now = SimpleFunction.nullary[LocalDateTime]("getdate")

  val addMillis = SimpleExpression.binary[Long, LocalDateTime, LocalDateTime] { (number, dateTime, builder) =>
    builder.sqlBuilder += "dateadd(millisecond,"
    builder.expr(number)
    builder.sqlBuilder += ","
    builder.expr(dateTime)
    builder.sqlBuilder += ")"
  }
}
