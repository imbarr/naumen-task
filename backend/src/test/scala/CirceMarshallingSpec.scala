import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.FlatSpec
import util.CirceMarshalling._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}

class CirceMarshallingSpec extends FlatSpec {
  implicit val system = ActorSystem("FlatSpec")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  case class Class(string: String, int: Int, boolean: Boolean)

  implicit val decoder = deriveDecoder[Class]
  implicit val encoder = deriveEncoder[Class]

  val instance = Class("text", 89, boolean = false)

  "Unmarshalling" should "work for every class with decoder" in {
    val entity = HttpEntity(ContentTypes.`application/json`, instance.asJson.noSpaces)
    val request = HttpRequest(HttpMethods.POST, entity = entity)
    val result = fromMessageUnmarshaller.apply(request)
    assert(fromFuture(result).contains(instance))
  }

  "Marshalling" should "work for every class with encoder" in {
    val result = fromFuture(Marshal(instance).to[HttpResponse])
    assert(result.isRight)
    val response = result.right.get
    assert(response.status == StatusCodes.OK)
    assert(response.entity.contentType == ContentTypes.`application/json`)
    val strict = fromFuture(response.entity.toStrict(FiniteDuration(10, TimeUnit.SECONDS)))
    val content = strict.right.get.data.utf8String
    val json = parse(content)
    assert(json.isRight)
    val decoded = json.right.get.as[Class]
    assert(decoded.contains(instance))
  }

  def fromFuture[T](future: Future[T]): Either[Throwable, T] =
    Await.ready(future, Duration.Inf).value.get.toEither
}
