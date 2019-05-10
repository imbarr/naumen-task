import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpMethods, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, MethodRejection, Route}
import data._
import data.Implicits._
import storage.InMemoryBook
import util.CirceMarshalling._

class Server(book: InMemoryBook) extends HttpApp{
  override def routes: Route =
    pathPrefix("phonebook") {
      pathEndOrSingleSlash {
        post {
          entity(as[BookEntry]) {entry =>
            val id = book.add(entry)
            complete(HttpResponse(StatusCodes.Created, List(Location("/phonebook/" + id))))
          }
        } ~
        get {
          parameters('nameSubstring) { substring =>
            complete(book.findByNameSubstring(substring))
          } ~
          parameters('phoneSubstring) { substring =>
            complete(book.findByPhoneNumberSubstring(substring))
          } ~
          complete(book.getAll)
        }
      } ~
      path(IntNumber) {
        id =>
          patch {
            entity(as[BookEntry]) { entry =>
              complete {
                if(book.replace(id, entry.name, entry.phoneNumber))
                  HttpResponse(StatusCodes.OK)
                else
                  HttpResponse(StatusCodes.NotFound)
              }
            } ~
            entity(as[NameWrapper]) { wrapper =>
              complete {
                if(book.changeName(id, wrapper.name))
                  HttpResponse(StatusCodes.OK)
                else
                  HttpResponse(StatusCodes.NotFound)
              }
            } ~
            entity(as[PhoneNumberWrapper]) { wrapper =>
              complete {
                if(book.changePhoneNumber(id, wrapper.phoneNumber))
                  HttpResponse(StatusCodes.OK)
                else
                  HttpResponse(StatusCodes.NotFound)
              }
            }
          } ~
          delete {
            complete {
              if(book.remove(id))
                HttpResponse(StatusCodes.OK)
              else
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
    }
}
