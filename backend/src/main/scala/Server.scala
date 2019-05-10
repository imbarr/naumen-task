import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, HttpApp, RejectionHandler, Route}
import com.typesafe.scalalogging.Logger
import data.Implicits._
import data._
import storage.InMemoryBook
import util.CirceMarshalling._

class Server(book: InMemoryBook)(implicit log: Logger) extends HttpApp {
  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case error =>
      log.error("Error occurred while processing request.", error)
      complete(StatusCodes.InternalServerError)
  }

  override val routes: Route =
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
    if (predicate)
      complete(StatusCodes.OK)
    else
      reject()
}
