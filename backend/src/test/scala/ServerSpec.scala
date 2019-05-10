import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import data.{BookEntry, BookEntryWithId, NameWrapper, PhoneNumberWrapper}
import org.scalatest.{Assertion, BeforeAndAfter, FlatSpec}
import storage.InMemoryBook
import data.Implicits._
import util.CirceMarshalling._

class ServerSpec extends FlatSpec with ScalatestRouteTest with BeforeAndAfter {
  implicit val log = Logger("server-spec")

  val entries = List(
    BookEntry("John Doe", "88005553535"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Someone Else", "dfgdgdgdfgd")
  )

  var server: Server = _
  var added: List[BookEntryWithId] = _

  before {
    val book = new InMemoryBook()
    added = entries.map(entry => {
      val id = book.add(entry)
      BookEntryWithId(id, entry.name, entry.phoneNumber)
    })
    server = new Server(book)
  }

  "Server" should "return all phonebook entries" in
    Get("/phonebook") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      val withoutIds = result.map(entry => BookEntry(entry.name, entry.phoneNumber))
      assert(withoutIds == entries)
    }

  it should "create new phonebook entry" in
    Post("/phonebook", BookEntry("New", "223424234")) ~> server.routes ~> check {
      assert(status == StatusCodes.Created)
      assert(header("Location").isDefined)
    }

  it should "find entries by name substring" in {
    Get("/phonebook?nameSubstring=Doe") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result.length == 3)
    }
    Get("/phonebook?nameSubstring=John") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result.length == 1)
    }
  }

  it should "find entries by telephone substring" in {
    Get("/phonebook?phoneSubstring=%2B7") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result.length == 2)
    }

    Get("/phonebook?phoneSubstring=") ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result.length == entries.length)
    }
  }

  it should "update entry" in {
    val id = added.head.id
    val requests = List(
      Patch("/phonebook/" + id, BookEntry("New name", "New phone")),
      Patch("/phonebook/" + id, NameWrapper("New name")),
      Patch("/phonebook/" + id, PhoneNumberWrapper("New name"))
    )

    testForAll(requests) {
      assert(status == StatusCodes.OK)
    }
  }

  it should "delete entry" in {
    val id = added.head.id
    Delete("/phonebook/" + id) ~> server.routes ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  it should "return 404 for non-existing resources" in {
    val id = added.map(_.id).max + 1
    val requests = List(
      Get("/something"),
      Delete("/phonebook/" + id)
    )

    testForAll(requests) {
      assert(status == StatusCodes.NotFound)
    }
  }

  it should "return 405 for not allowed methods" in {
    val id = added.head.id
    val requests = List(
      Delete("/phonebook"),
      Patch("/phonebook"),
      Get("/phonebook/" + id),
      Put("/phonebook/" + id)
    )

    testForAll(requests) {
      assert(status == StatusCodes.MethodNotAllowed)
    }
  }

  it should "return 400 for incorrect requests" in {
    val id = added.head.id
    val requests = List(
      Post("/phonebook", HttpEntity(ContentTypes.`application/json`, "")),
      Post("/phonebook", NameWrapper("name")),
      Patch("/phonebook/" + id, HttpEntity(ContentTypes.`application/json`, "{\"malformed\": json}")),
      Patch("/phonebook/" + id, "{\"some\": \"json\"}")
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
