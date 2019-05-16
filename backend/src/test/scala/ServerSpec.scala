import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import data.Implicits._
import data.{BookEntry, BookEntryWithId, NameWrapper, PhoneNumberWrapper}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Assertion, BeforeAndAfter, FlatSpec}
import storage.Book
import util.CirceMarshalling._

import scala.concurrent.Future

class ServerSpec extends FlatSpec with ScalatestRouteTest with BeforeAndAfter with MockFactory {
  implicit val log = Logger("naumen-task-test")

  var book: Book = _
  var server: Server = _

  before {
    book = mock[Book]
    server = new Server(book)
  }

  "Server" should "return all phonebook entries" in {
    val entries = List(
      BookEntryWithId(22, "John", "343453"),
      BookEntryWithId(11, "Boris", "3354354")
    )
    book.getAll _ expects() returning Future.successful(entries)
    Get("/phonebook") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entries)
    }
  }

  it should "return range of entries" in {
    val start = 11
    val end = 12
    val entries = List(
      BookEntryWithId(22, "John", "343453"),
      BookEntryWithId(11, "Boris", "3354354")
    )
    book.getRange _ expects (start, end) returning Future.successful(entries)
    val uri = Uri("/phonebook").withQuery(Query("start" -> start.toString, "end" -> end.toString))
    Get(uri) ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entries)
      val expectedHeader = `Content-Range`(RangeUnits.Other("entries"), ContentRange(start, end))
      assert(header("Content-Range").contains(expectedHeader))
    }
  }

  it should "create new phonebook entry" in {
    val entry = BookEntry("Bob", "dgfdgfdf")
    val id = 122
    book.add _ expects entry returning Future.successful(id)
    Post("/phonebook", entry) ~> server.routes ~> check {
      assert(status == StatusCodes.Created)
      assert(header("Location").contains(Location(s"/phonebook/$id")))
    }
  }

  it should "find entries by name substring" in {
    val substring = "Doe"
    val foundEntries = List(
      BookEntryWithId(11, "John Doe", "456456"),
      BookEntryWithId(22, "Jane Doe", "232323")
    )
    book.findByNameSubstring _ expects substring returning Future.successful(foundEntries)

    val uri = Uri("/phonebook").withQuery(Query("nameSubstring" -> substring))
    Get(uri) ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == foundEntries)
    }
  }

  it should "find entries by telephone substring" in {
    val substring = "+7"
    val foundEntries = List(
      BookEntryWithId(11, "John Doe", "+7456456"),
      BookEntryWithId(22, "Jane Doe", "+7232323")
    )
    book.findByPhoneNumberSubstring _ expects substring returning Future.successful(foundEntries)

    val uri = Uri("/phonebook").withQuery(Query("phoneSubstring" -> substring))
    Get(uri) ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == foundEntries)
    }
  }

  it should "update entry" in {
    val id = 11
    val name = "name"
    val phone = "8995235523"
    book.replace _ expects(id, name, phone) returning Future.successful(true) noMoreThanOnce()
    book.changeName _ expects(id, name) returning Future.successful(true) noMoreThanOnce()
    book.changePhoneNumber _ expects(id, phone) returning Future.successful(true) noMoreThanOnce()
    val requests = List(
      Patch(s"/phonebook/$id", BookEntry(name, phone)),
      Patch(s"/phonebook/$id", NameWrapper(name)),
      Patch(s"/phonebook/$id", PhoneNumberWrapper(phone))
    )

    testForAll(requests) {
      assert(status == StatusCodes.OK)
    }
  }

  it should "delete entry" in {
    val id = 11
    book.remove _ expects id returning Future.successful(true)
    Delete(s"/phonebook/$id") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  it should "return 404 for non-existing resources" in {
    val id = 20
    val name = "name"
    val phone = "345353"
    book.remove _ expects id returning Future.successful(false) anyNumberOfTimes()
    book.replace _ expects(id, name, phone) returning Future.successful(false) anyNumberOfTimes()
    val requests = List(
      Get("/something"),
      Delete(s"/phonebook/$id"),
      Patch(s"/phonebook/$id", BookEntry(name, phone))
    )

    testForAll(requests) {
      assert(status == StatusCodes.NotFound)
    }
  }

  it should "return 405 for not allowed methods" in {
    val id = 22
    val requests = List(
      Delete("/phonebook"),
      Patch("/phonebook"),
      Get(s"/phonebook/$id"),
      Put(s"/phonebook/$id")
    )

    testForAll(requests) {
      assert(status == StatusCodes.MethodNotAllowed)
    }
  }

  it should "return 400 for incorrect requests" in {
    val id = 22
    val requests = List(
      Post("/phonebook", HttpEntity(ContentTypes.`application/json`, "")),
      Post("/phonebook", NameWrapper("name")),
      Patch(s"/phonebook/$id", HttpEntity(ContentTypes.`application/json`, "{\"malformed\": json}")),
      Patch(s"/phonebook/$id", "{\"some\": \"json\"}"),
      Get(Uri("/phonebook").withQuery(Query("start" -> "11", "end" -> "lul"))),
      Get(Uri("/phonebook").withQuery(Query("start" -> "32", "end" -> "22")))
    )

    testForAll(requests) {
      assert(status == StatusCodes.BadRequest)
    }
  }

  private def testForAll(requests: List[HttpRequest])(assertion: => Assertion): Unit =
    for (request <- requests)
      request ~> Route.seal(server.routes) ~> check {
        assertion
      }
}
