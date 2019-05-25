import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import config.CacheConfig
import data.BookEntryWithId
import data.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec}
import storage.Book
import util.CirceMarshalling._
import util.TestUtils._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class ServerWithCachingSpec extends FlatSpec with ScalatestRouteTest with BeforeAndAfter with MockFactory {
  implicit val log = Logger("naumen-task-test")
  implicit val executionContext = ExecutionContext.global

  val cacheConfig = CacheConfig(10, 10, Duration.Inf, Duration.Inf)

  var book: Book = _
  var dataSaver: DataSaver = _
  var taskManager: TaskManager = _
  var server: Server = _

  before {
    book = mock[Book]
    dataSaver = mock[DataSaver]
    taskManager = mock[TaskManager]
    server = new Server(book, dataSaver, taskManager, Some(cacheConfig))
  }

  "Server (with caching)" should "cache responses for GET requests" in {
    book.get _ expects(None, None, None) returning Future.successful(entriesWithIds)
    for (_ <- 1 to 3) {
      Get("/phonebook") ~> server.route ~> check {
        assert(status == StatusCodes.OK)
        val result = responseAs[List[BookEntryWithId]]
        assert(result == entriesWithIds)
      }
    }
  }

  it should "not cache responses for another methods" in {
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
}
