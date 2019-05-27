import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import config.CacheConfig
import data.Implicits._
import data._
import filesystem.DataSaver
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec}
import server.{Server, TaskManager}
import storage.Book
import util.CirceMarshalling._
import util.TestData.{entriesWithIds, entry}
import util.TestUtils._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class ServerSpec extends FlatSpec with ScalatestRouteTest with BeforeAndAfter with MockFactory {
  implicit val log = Logger("naumen-task-test")
  implicit val executionContext = ExecutionContext.global

  val cacheConfig = CacheConfig(10, 10, Duration.Inf, Duration.Inf)

  "Server" should "return all phonebook entries" in serverTest { (book, _, _, server) =>
    book.getEntries _ expects(None, None, None) returning Future.successful(entriesWithIds)
    Get("/phonebook") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entriesWithIds)
    }
  }

  it should "return range of entries" in serverTest { (book, _, _, server) =>
    val start = 11
    val end = 12
    val total = 100
    book.getSize _ expects(*, *) returning Future.successful(total)
    book.getEntries _ expects(None, None, Some((start, end))) returning Future.successful(entriesWithIds)
    val uri = Uri("/phonebook").withQuery(Query("start" -> start.toString, "end" -> end.toString))
    Get(uri) ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entriesWithIds)
      val expectedHeader = RawHeader("X-Total-Count", total.toString)
      assert(header("X-Total-Count").contains(expectedHeader))
    }
  }

  it should "return entry by id" in serverTest { (book, _, _, server) =>
    val id = 22
    book.getById _ expects id returning Future.successful(Some(entry))
    Get(s"/phonebook/$id") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[BookEntry]
      assert(result == entry)
    }
  }

  it should "create new phonebook entry" in serverTest { (book, _, _, server) =>
    val id = 122
    book.add _ expects entry returning Future.successful(Some(id))
    Post("/phonebook", entry) ~> server.route ~> check {
      assert(status == StatusCodes.Created)
      assert(header("Location").contains(Location(s"/phonebook/$id")))
    }
  }

  it should "find entries by name substring" in serverTest { (book, _, _, server) =>
    val substring = "Doe"
    book.getEntries _ expects(Some(substring), None, None) returning Future.successful(entriesWithIds)

    val uri = Uri("/phonebook").withQuery(Query("nameSubstring" -> substring))
    Get(uri) ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entriesWithIds)
    }
  }

  it should "find entries by telephone substring" in serverTest { (book, _, _, server) =>
    val substring = "+7"
    book.getEntries _ expects(None, Some(substring), None) returning Future.successful(entriesWithIds)

    val uri = Uri("/phonebook").withQuery(Query("phoneSubstring" -> substring))
    Get(uri) ~> server.route ~> check {
      assert(status == StatusCodes.OK)
      val result = responseAs[List[BookEntryWithId]]
      assert(result == entriesWithIds)
    }
  }

  it should "update entry" in serverTest { (book, _, _, server) =>
    val id = 11
    val name = "name"
    val phone = entry.phone
    book.replace _ expects(id, name, phone) returning Future.successful(true)
    book.changeName _ expects(id, name) returning Future.successful(true)
    book.changePhoneNumber _ expects(id, phone) returning Future.successful(true)
    val requests = List(
      Patch(s"/phonebook/$id", BookEntry(name, phone)),
      Patch(s"/phonebook/$id", NameWrapper(name)),
      Patch(s"/phonebook/$id", PhoneNumberWrapper(phone))
    )
    assertStatusForAll(server, requests, StatusCodes.OK)
  }

  it should "delete entry" in serverTest { (book, _, _, server) =>
    val id = 11
    book.remove _ expects id returning Future.successful(true)
    Delete(s"/phonebook/$id") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
    }
  }

  it should "save phonebook" in serverTest { (_, _, taskManager, server) =>
    val id = 22
    taskManager.add _ expects * returning Some(id)
    Post("/files") ~> server.route ~> check {
      assert(status == StatusCodes.Accepted)
      assert(header("Location").contains(Location(s"/tasks/$id")))
    }
  }

  it should "not save phonebook if another task in progress" in serverTest { (_, _, taskManager, server) =>
    taskManager.add _ expects * returning None
    Post("/files") ~> server.route ~> check {
      assert(status == StatusCodes.TooManyRequests)
    }
  }

  it should "return status of task in progress" in
    taskStatusTest(longRunning, TaskStatus("in progress"))

  it should "return status of completed task" in
    taskStatusTest(Future.successful(), TaskStatus("completed"))

  it should "return status of failed task" in {
    val message = "some message"
    val taskResult = Future.failed(new Exception(message))
    val expectedStatus = TaskStatus("failed", Some("some message"))
    taskStatusTest(taskResult, expectedStatus)
  }

  it should "return 404 for non-existing resources" in serverTest { (book, _, taskManager, server) =>
    val id = 20
    val name = "name"
    val phone = entry.phone
    book.remove _ expects id returning Future.successful(false) anyNumberOfTimes()
    book.replace _ expects(id, name, phone) returning Future.successful(false) anyNumberOfTimes()
    book.getById _ expects id returning Future.successful(None) anyNumberOfTimes()
    taskManager.get _ expects id returning None
    val requests = List(
      Get("/something"),
      Get(s"/phonebook/$id"),
      Get(s"/tasks/$id"),
      Delete(s"/phonebook/$id"),
      Patch(s"/phonebook/$id", BookEntry(name, phone))
    )
    assertStatusForAll(server, requests, StatusCodes.NotFound)
  }

  it should "return 405 for not allowed methods" in serverTest { (_, _, _, server) =>
    val requests = List(
      Delete("/phonebook"),
      Patch("/phonebook"),
      Put(s"/phonebook/22"),
      Get("/files")
    )
    assertStatusForAll(server, requests, StatusCodes.MethodNotAllowed)
  }

  it should "return 400 for incorrect requests" in serverTest { (_, _, _, server) =>
    val requests = List(
      Post("/phonebook", HttpEntity(ContentTypes.`application/json`, "")),
      Post("/phonebook", NameWrapper("name")),
      Patch(s"/phonebook/22", HttpEntity(ContentTypes.`application/json`, "{\"malformed\": json}")),
      Patch(s"/phonebook/22", "{\"some\": \"json\"}"),
      Get(Uri("/phonebook").withQuery(Query("start" -> "11", "end" -> "lul"))),
      Get(Uri("/phonebook").withQuery(Query("start" -> "32", "end" -> "22"))),
      Get(Uri("/phonebook").withQuery(Query("start" -> "50"))),
      Post("/phonebook", Map("name" -> "John", "phone" -> "88005553535"))
    )
    assertStatusForAll(server, requests, StatusCodes.BadRequest)
  }

  it should "return 409 for same entry added" in serverTest { (book, _, _, server) =>
    book.add _ expects entry returning Future.successful(None)
    Post("/phonebook", entry) ~> server.route ~> check {
      assert(status == StatusCodes.Conflict)
    }
  }

  "Server (with caching)" should "cache responses for GET requests" in serverTestWithCaching { (book, _, _, server) =>
    book.getEntries _ expects(None, None, None) returning Future.successful(entriesWithIds)
    for (_ <- 1 to 3) {
      Get("/phonebook") ~> server.route ~> check {
        assert(status == StatusCodes.OK)
        val result = responseAs[List[BookEntryWithId]]
        assert(result == entriesWithIds)
      }
    }
  }

  it should "not cache responses for another methods" in serverTestWithCaching { (book, _, _, server) =>
    val id = 11
    book.remove _ expects id returning Future.successful(true)
    book.remove _ expects id returning Future.successful(false)
    Delete(s"/phonebook/$id") ~> server.route ~> check {
      assert(status == StatusCodes.OK)
    }
    Delete(s"/phonebook/$id") ~> Route.seal(server.route) ~> check {
      assert(status == StatusCodes.NotFound)
    }
  }

  private def serverTest(test: (Book, DataSaver, TaskManager, Server) => Unit): Unit =
    serverTest(None, test)

  private def serverTestWithCaching(test: (Book, DataSaver, TaskManager, Server) => Unit): Unit =
    serverTest(Some(cacheConfig), test)

  private def serverTest(cacheConfig: Option[CacheConfig],
                         test: (Book, DataSaver, TaskManager, Server) => Unit): Unit = {
    val book = mock[Book]
    val dataSaver = mock[DataSaver]
    val taskManager = mock[TaskManager]
    val server = new Server(book, dataSaver, taskManager, cacheConfig)
    test(book, dataSaver, taskManager, server)
  }

  private def assertStatusForAll(server: Server, requests: List[HttpRequest], statusCode: StatusCode): Unit =
    for (request <- requests)
      request ~> Route.seal(server.route) ~> check {
        assert(status == statusCode)
      }

  private def taskStatusTest(taskResult: Future[Unit], expectedStatus: TaskStatus): Unit =
    serverTest { (_, _, taskManager, server) =>
      val id = 1
      taskManager.get _ expects id returning Some(taskResult)
      Get(s"/tasks/$id") ~> server.route ~> check {
        assert(status == StatusCodes.OK)
        val result = responseAs[TaskStatus]
        assert(result == expectedStatus)
      }
    }


}
