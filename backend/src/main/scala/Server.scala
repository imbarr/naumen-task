import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route}
import data.Implicits._
import data._
import storage.InMemoryBook
import util.CirceMarshalling._

class Server(book: InMemoryBook) extends HttpApp {
  override val routes: Route =
    pathPrefix("phonebook") {
      pathEndOrSingleSlash {
        phonebookRoute
      } ~
        path(IntNumber) {
          phonebookEntryRoute
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
