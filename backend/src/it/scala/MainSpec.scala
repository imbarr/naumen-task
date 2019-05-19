import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import config.Config
import data.Implicits._
import data.{BookEntry, BookEntryWithId}
import io.circe.Encoder
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.FlatSpec
import pureconfig.generic.auto._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

class MainSpec extends FlatSpec {
  implicit val system = ActorSystem("it-tests")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  Main.main(Array.empty)

  "Application" should "add and return entries" in {
    val config = pureconfig
      .loadConfig[Config]
      .fold(_ => fail("Failed to load config"), identity)
      .server
    val uri = s"http://${config.interface}:${config.port}/phonebook"

    val entries = Set(
      BookEntry("test entry 1", "111"),
      BookEntry("test entry 2", "111")
    )

    for (entry <- entries) {
      val response = send(HttpRequest(HttpMethods.POST, uri, entity = toEntity(entry)))
      assert(response.status == StatusCodes.Created)
    }
    val response3 = send(HttpRequest(HttpMethods.GET, uri))
    assert(response3.status == StatusCodes.OK)
    val strictFuture = response3.entity.toStrict(FiniteDuration(10, TimeUnit.SECONDS))
    val content = Await.result(strictFuture, Duration.Inf).data.utf8String
    val decodedEither = parse(content).flatMap(_.as[List[BookEntryWithId]])
    assert(decodedEither.isRight)
    val decoded = decodedEither.right.get
    val withoutIds = decoded.map(entry => BookEntry(entry.name, entry.phone))
    assert(entries.subsetOf(withoutIds.toSet))
  }

  private def send(request: HttpRequest): HttpResponse = {
    val responseFuture = Http().singleRequest(request)
    Await.result(responseFuture, Duration.Inf)
  }

  private def toEntity[T: Encoder](content: T): RequestEntity =
    HttpEntity(ContentTypes.`application/json`, content.asJson.noSpaces)
}
