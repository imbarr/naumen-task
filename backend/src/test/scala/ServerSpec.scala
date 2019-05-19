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
    book.get _ expects(None, None, None) returning Future.successful(entries)
    Get("/phonebook") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entries)
    }
  }

  it should "return range of entries" in {
    val start = 11
    val end = 12
    val total = 100
    val entries = List(
      BookEntryWithId(22, "John", "343453"),
      BookEntryWithId(11, "Boris", "3354354")
    )
    book.getSize _ expects(*, *) returning Future.successful(total)
    book.get _ expects(None, None, Some((start, end))) returning Future.successful(entries)
    val uri = Uri("/phonebook").withQuery(Query("start" -> start.toString, "end" -> end.toString))
    Get(uri) ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entries)
      val expectedHeader = RawHeader("X-Total-Count", total.toString)
      assert(header("X-Total-Count").contains(expectedHeader))
    }
  }

  it should "return entry by id" in {
    val id = 22
    val entry = BookEntry("Boris", "33434344")
    book.getById _ expects id returning Future.successful(Some(entry))
    Get(s"/phonebook/$id") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[BookEntry]
      assert(result == entry)
    }
  }

  it should "create new phonebook entry" in {
    val entry = BookEntry("Bob", "dgfdgfdf")
    val id = 122
    book.add _ expects entry returning Future.successful(id)
    Post("/phonebook", entry) ~> server.route ~> check {
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
    book.get _ expects(Some(substring), None, None) returning Future.successful(foundEntries)

    val uri = Uri("/phonebook").withQuery(Query("nameSubstring" -> substring))
    Get(uri) ~> server.route ~> check {
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
    book.get _ expects(None, Some(substring), None) returning Future.successful(foundEntries)

    val uri = Uri("/phonebook").withQuery(Query("phoneSubstring" -> substring))
    Get(uri) ~> server.route ~> check {
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
    Delete(s"/phonebook/$id") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  it should "return 404 for non-existing resources" in {
    val id = 20
    val name = "name"
    val phone = "345353"
    book.remove _ expects id returning Future.successful(false) anyNumberOfTimes()
    book.replace _ expects(id, name, phone) returning Future.successful(false) anyNumberOfTimes()
    book.getById _ expects id returning Future.successful(None) anyNumberOfTimes()
    val requests = List(
      Get("/something"),
      Get(s"/phonebook/$id"),
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
      Put(s"/phonebook/$id")
    )

    testForAll(requests) {
      assert(status == StatusCodes.MethodNotAllowed)
    }
  }

  it should "return 400 for incorrect requests" in {
    val requests = List(
      Post("/phonebook", HttpEntity(ContentTypes.`application/json`, "")),
      Post("/phonebook", NameWrapper("name")),
      Patch(s"/phonebook/22", HttpEntity(ContentTypes.`application/json`, "{\"malformed\": json}")),
      Patch(s"/phonebook/22", "{\"some\": \"json\"}"),
      Get(Uri("/phonebook").withQuery(Query("start" -> "11", "end" -> "lul"))),
      Get(Uri("/phonebook").withQuery(Query("start" -> "32", "end" -> "22"))),
      Get(Uri("/phonebook").withQuery(Query("start" -> "50")))
    )

    testForAll(requests) {
      assert(status == StatusCodes.BadRequest)
    }
  }

  private def testForAll(requests: List[HttpRequest])(assertion: => Assertion): Unit =
    for (request <- requests)
      request ~> Route.seal(server.route) ~> check {
        assertion
      }
}
