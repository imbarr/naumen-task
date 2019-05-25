package util

import data.{BookEntry, BookEntryWithId, Phone}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object TestUtils {
  case class Class(string: String, int: Int, boolean: Boolean)
  implicit val encoder = deriveEncoder[Class]
  implicit val decoder = deriveDecoder[Class]

  val entries = List(
    BookEntry("John Doe", Phone.fromString("+78005553535").right.get),
    BookEntry("Jane Doe", Phone.fromString("+79223101010").right.get),
    BookEntry("Jane Doe", Phone.fromString("+79273101010").right.get),
    BookEntry("Someone Else", Phone.fromString("+79223101010").right.get)
  )
  val anotherEntries = entries.map(entry => BookEntry(entry.name + "!", entry.phone))
  val entriesWithIds = List(
    BookEntryWithId(22, "John", Phone.fromString("+7 800 5553535").right.get),
    BookEntryWithId(11, "Boris", Phone.fromString("+78005553535").right.get),
    BookEntryWithId(121, "Victor", Phone.fromString("+79228222201").right.get)
  )
  val entryWithId = entriesWithIds.head
  val entry = entries.head

  def await[T](future: Future[T]): T =
    Await.result(future, Duration.Inf)

  def longRunning(implicit executionContext: ExecutionContext): Future[Nothing] = Future {
    Thread.sleep(1000)
    throw new Exception()
  }
}
