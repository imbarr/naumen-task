package server

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.directives.CachingDirectives._
import util.CirceMarshalling._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import config.{CacheConfig, ServerConfig}
import data.Implicits._
import data._
import filesystem.DataSaver
import storage.Book

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Server(book: Book, dataSaver: DataSaver, taskManager: TaskManager, caching: Option[CacheConfig] = None)
            (implicit log: Logger, system: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  val keyFunction: PartialFunction[RequestContext, Uri] = {
    case context: RequestContext
      if context.request.method == HttpMethods.GET => context.request.uri
  }

  val CORSHeaders = List(
    // Wildcard as allowed origin is vulnerable to cross-site request forgery.
    // It is left like this for easier application setup.
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Headers`("Content-Type"),
    `Access-Control-Expose-Headers`("X-Total-Count", "Location")
  )

  val allowedMethodsHeader = `Access-Control-Allow-Methods`(
    HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST, HttpMethods.PATCH, HttpMethods.DELETE
  )

  def start(config: ServerConfig): Future[ServerBinding] =
    Http().bindAndHandle(route2HandlerFlow(route), config.interface, config.port)

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case error =>
      log.error("Error occurred while processing request", error)
      complete(StatusCodes.InternalServerError)
  }

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case MalformedRequestContentRejection(_, cause) =>
      val message = cause.getMessage.split('\n').head.split(':').last.trim
      complete(StatusCodes.BadRequest, message)
    }.result()

  val route: Route =
    caching match {
      case Some(cacheConfig) =>
        cache(getCache(cacheConfig), keyFunction) {
          rootWithCORS
        }
      case None =>
        rootWithCORS
    }

  private def rootWithCORS: Route =
    respondWithHeaders(CORSHeaders) {
      options {
        respondWithHeader(allowedMethodsHeader) {
          complete(StatusCodes.OK)
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
      path("files") {
        files
      } ~
      pathPrefix("tasks") {
        path(IntNumber) {
          taskWithId
        }
      } ~
      path("ffoknit") {
        complete(StatusCodes.ImATeapot)
      }

  private def phonebook: Route =
    post {
      entity(as[BookEntry]) {
        createEntry
      }
    } ~
      get {
        getEntries
      }

  private def phonebookEntry(id: Int): Route =
    get {
      getEntry(id)
    } ~
      patch {
        modifyEntry(id)
      } ~
      delete {
        notFoundIfFalse {
          book.remove(id)
        }
      }

  private def files: Route =
    post {
      lazy val getAll = book.get()
      lazy val task = getAll.flatMap(dataSaver.save("phonebook", _))
      taskManager.add(task) match {
        case Some(id) =>
          respondWithHeader(Location(s"/tasks/$id")) {
            complete(StatusCodes.Accepted)
          }
        case None =>
          complete(StatusCodes.TooManyRequests, "Maximum number of asynchronous tasks exceeded")
      }
    }

  private def taskWithId(id: Int): Route =
    get {
      taskManager.get(id) match {
        case None => reject()
        case Some(task) =>
          taskRoute(task)
      }
    }

  private def createEntry(entry: BookEntry): Route = {
    onSuccess(book.add(entry)) {
      case Some(id) =>
        respondWithHeader(Location(s"/phonebook/$id")) {
          complete(StatusCodes.Created)
        }
      case None =>
        complete(StatusCodes.Conflict, "phonebook already contains this entry")
    }
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

  private def getEntry(id: Int): Route =
    onSuccess(book.getById(id)) {
      case None => reject()
      case Some(entry) => complete(entry)
    }

  private def modifyEntry(id: Int): Route =
    entity(as[BookEntry]) { entry =>
      notFoundIfFalse {
        book.replace(id, entry.name, entry.phone)
      }
    } ~
      entity(as[NameWrapper]) { wrapper =>
        notFoundIfFalse {
          book.changeName(id, wrapper.name)
        }
      } ~
      entity(as[PhoneNumberWrapper]) { wrapper =>
        notFoundIfFalse {
          book.changePhoneNumber(id, wrapper.phone)
        }
      }

  private def taskRoute(task: Future[Unit]): Route =
    complete {
      task.value match {
        case Some(Success(_)) => TaskStatus("completed")
        case Some(Failure(error)) => TaskStatus("failed", Some(error.getMessage))
        case None => TaskStatus("in progress")
      }
    }

  private def getEntriesInRange(nameSubstring: Option[String],
                                phoneSubstring: Option[String],
                                start: Int,
                                end: Int): Route = {
    if (start > end) {
      reject(MalformedQueryParamRejection("end", "start cannot be greater then end"))
    }
    else {
      val totalFuture = book.getSize(nameSubstring, phoneSubstring)
      onSuccess(totalFuture) { total =>
        val totalHeader = RawHeader("X-Total-Count", total.toString)
        respondWithHeader(totalHeader) {
          val result = book.get(nameSubstring, phoneSubstring, Some((start, end)))
          complete(result)
        }
      }
    }
  }

  private def notFoundIfFalse(predicate: Future[Boolean]): Route =
    Route.seal {
      onSuccess(predicate) { success =>
        if (success)
          complete(StatusCodes.OK)
        else
          reject()
      }
    }

  private def getCache(cacheConfig: CacheConfig): Cache[Uri, RouteResult] = {
    val settings = CachingSettings(system)
    val lfuSettings = settings.lfuCacheSettings
      .withMaxCapacity(cacheConfig.maxCapacity)
      .withInitialCapacity(cacheConfig.initialCapacity)
      .withTimeToLive(cacheConfig.timeToLive)
      .withTimeToIdle(cacheConfig.timeToIdle)
    val newSettings = settings.withLfuCacheSettings(lfuSettings)
    LfuCache(newSettings)
  }
}
