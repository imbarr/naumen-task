import _root_.util.CirceMarshalling._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.ServerConfig
import data.Implicits._
import data._
import storage.Book

import scala.concurrent.Future

class Server(book: Book)(implicit log: Logger, system: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  val CORSHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With", "X-Total-Count"),
    `Access-Control-Expose-Headers`("X-Total-Count")
  )

  def start(config: ServerConfig): Future[ServerBinding] =
    Http().bindAndHandle(route2HandlerFlow(route), config.interface, config.port)

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case error =>
      log.error("Error occurred while processing request.", error)
      complete(StatusCodes.InternalServerError)
  }

  def route: Route =
    respondWithHeaders(CORSHeaders) {
      options {
        respondWithHeader(`Access-Control-Allow-Methods`(HttpMethods.OPTIONS, HttpMethods.GET)) {
          complete()
        }
      } ~
        Route.seal {
          root
        }
    }

  private def root: Route =
    pathPrefix("phonebook") {
      pathEndOrSingleSlash {
        phonebook
      } ~
        path(IntNumber) {
          phonebookEntry
        }
    } ~
      path("ffoknit") {
        complete(StatusCodes.ImATeapot)
      }

  private def phonebook: Route =
    post {
      createEntry
    } ~
      get {
        getEntries
      }

  private def phonebookEntry(id: Int): Route =
    patch {
      modifyEntry(id)
    } ~
      delete {
        predicate {
          book.remove(id)
        }
      }

  private def createEntry: Route =
    entity(as[BookEntry]) { entry =>
      val futureId = book.add(entry)
      val futureHeaders = futureId.map(id => List(Location(s"/phonebook/$id")))
      val futureResponse = futureHeaders.map(headers => HttpResponse(StatusCodes.Created, headers))
      complete(futureResponse)
    }

  private def getEntries: Route =
    parameters('nameSubstring.?, 'phoneSubstring.?, 'start.as[Int].?, 'end.as[Int].?) {
      (nameSubstring, phoneSubstring, startOption, endOption) =>
        (startOption, endOption) match {
          case (None, None) =>
            complete(book.get(nameSubstring, phoneSubstring))
          case (None, _) | (_, None) =>
            reject(MalformedQueryParamRejection("start", "start and end parameters should be used together"))
          case (Some(start), Some(end)) =>
            getEntriesInRange(nameSubstring, phoneSubstring, start, end)
        }
    }

  private def modifyEntry(id: Int): Route =
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

  private def getEntriesInRange(nameSubstring: Option[String], phoneSubstring: Option[String], start: Int, end: Int): Route = {
    if (start > end) {
      reject(MalformedQueryParamRejection("end", "start cannot be greater then end"))
    }
    else {
      onSuccess(book.getSize) { total =>
        if (start >= total) {
          reject(MalformedQueryParamRejection("start", "start cannot be greater then total number of entries"))
        }
        else {
          respondWithHeader(RawHeader("X-Total-Count", total.toString)) {
            complete(book.get(nameSubstring, phoneSubstring, Some((start, end))))
          }
        }
      }
    }
  }

  private def predicate(predicate: Future[Boolean]): Route =
    Route.seal {
      onSuccess(predicate) { success =>
        if (success)
          complete(StatusCodes.OK)
        else
          reject()
      }
    }
}
