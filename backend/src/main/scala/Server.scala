import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.ServerConfig
import data.Implicits._
import data._
import storage.Book
import util.CirceMarshalling._

import scala.concurrent.Future

class Server(book: Book)(implicit log: Logger) {
  implicit val system = ActorSystem("naumen-task")
  implicit val materializer = ActorMaterializer()

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
      val id = book.add(entry)
      val headers = List(Location("/phonebook/" + id))
      complete(HttpResponse(StatusCodes.Created, headers))
    }

  val getEntries: Route =
    parameters('nameSubstring) { substring =>
      complete(book.findByNameSubstring(substring))
    } ~
      parameters('phoneSubstring) { substring =>
        complete(book.findByPhoneNumberSubstring(substring))
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

  def predicate(predicate: => Boolean): Route =
    Route.seal {
      if (predicate)
        complete(StatusCodes.OK)
      else
        reject()
    }
}
