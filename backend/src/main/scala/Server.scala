import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.{ExceptionHandler, MalformedQueryParamRejection, Route}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.ServerConfig
import data.Implicits._
import data._
import storage.Book
import util.CirceMarshalling._

import scala.concurrent.Future
import scala.util.Try

class Server(book: Book)(implicit log: Logger, system: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  def start(config: ServerConfig): Future[ServerBinding] =
    Http().bindAndHandle(route2HandlerFlow(routes), config.interface, config.port)

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case error =>
      log.error("Error occurred while processing request.", error)
      complete(StatusCodes.InternalServerError)
  }

  def routes: Route =
    pathPrefix("phonebook") {
      pathEndOrSingleSlash {
        phonebookRoute
      } ~
        path(IntNumber) {
          phonebookEntryRoute
        }
    } ~
      path("ffoknit") {
        complete(StatusCodes.ImATeapot)
      }

  def phonebookRoute: Route =
    post {
      createEntry
    } ~
      get {
        getEntries
      }

  def phonebookEntryRoute(id: Int): Route =
    patch {
      modifyEntry(id)
    } ~
      delete {
        predicate {
          book.remove(id)
        }
      }

  val createEntry: Route =
    entity(as[BookEntry]) { entry =>
      val futureId = book.add(entry)
      val futureHeaders = futureId.map(id => List(Location("/phonebook/" + id)))
      val futureResponse = futureHeaders.map(headers => HttpResponse(StatusCodes.Created, headers))
      complete(futureResponse)
    }

  val getEntries: Route =
    parameters('nameSubstring) { substring =>
      complete(book.findByNameSubstring(substring))
    } ~
      parameters('phoneSubstring) { substring =>
        complete(book.findByPhoneNumberSubstring(substring))
      } ~
      parameters('start, 'end) { (start, end) =>
        Route.seal {
          getRangeRoute(start, end)
        }
      } ~
      complete(book.getAll)

  def modifyEntry(id: Int): Route =
    entity(as[BookEntry]) { entry =>
      predicate {
        book.replace(id, entry.name, entry.phoneNumber)
      }
    } ~
      entity(as[NameWrapper]) { wrapper =>
        predicate {
          book.changeName(id, wrapper.name)
        }
      } ~
      entity(as[PhoneNumberWrapper]) { wrapper =>
        predicate {
          book.changePhoneNumber(id, wrapper.phoneNumber)
        }
      }

  def getRangeRoute(startString: String, endString: String): Route = {
    if (Try(startString.toInt).isFailure) {
      reject(MalformedQueryParamRejection("start", "start should be integer"))
    }
    else if(Try(endString.toInt).isFailure) {
      reject(MalformedQueryParamRejection("end", "end should be integer"))
    }
    else if(endString.toInt < startString.toInt) {
      reject(MalformedQueryParamRejection("end", "end should not be less then start"))
    }
    else {
      val start = startString.toInt
      val end = endString.toInt
      val range = book.getRange(start, end)
      val header = `Content-Range`(RangeUnits.Other("entries"), ContentRange(start, end))
      respondWithHeader(header) {
        complete(range)
      }
    }
  }

  def predicate(predicate: Future[Boolean]): Route =
    Route.seal {
      onSuccess(predicate) { success =>
        if (success)
          complete(StatusCodes.OK)
        else
          reject()
      }
    }
}
