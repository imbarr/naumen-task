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
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With", "Content-Range"),
    `Access-Control-Expose-Headers`("Content-Range")
  )

  def start(config: ServerConfig): Future[ServerBinding] =
    Http().bindAndHandle(route2HandlerFlow(routes), config.interface, config.port)

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case error =>
      log.error("Error occurred while processing request.", error)
      complete(StatusCodes.InternalServerError)
  }

  def routes: Route =
    respondWithHeaders(CORSHeaders) {
      options {
        respondWithHeader(`Access-Control-Allow-Methods`(HttpMethods.OPTIONS, HttpMethods.GET)) {
          complete()
        }
      } ~
        Route.seal {
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
        }
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
      val futureHeaders = futureId.map(id => List(Location(s"/phonebook/$id")))
      val futureResponse = futureHeaders.map(headers => HttpResponse(StatusCodes.Created, headers))
      complete(futureResponse)
    }

  val getEntries: Route =
    parameters('nameSubstring.?, 'phoneSubstring.?, 'start.as[Int].?, 'end.as[Int].?) { (nameSubstring, phoneSubstring, startOption, endOption) =>
      (startOption, endOption) match {
        case (None, _) | (_, None) =>
          complete(book.get(nameSubstring, phoneSubstring))
        case (Some(start), Some(end)) =>
          if (start > end)
            reject(MalformedQueryParamRejection("end", "start cannot be greater then end"))
          else
            onSuccess(book.getSize) { total =>
              if (start >= total)
                reject(MalformedQueryParamRejection("start", "start cannot be greater then total number of entries"))
              else {
                val actualEnd = Math.min(end, total)
                val header = `Content-Range`(RangeUnits.Other("entries"), ContentRange(start, actualEnd, total))
                respondWithHeader(header) {
                  complete(book.get(nameSubstring, phoneSubstring, Some((start, end))))
                }
              }
            }
      }
    }

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
