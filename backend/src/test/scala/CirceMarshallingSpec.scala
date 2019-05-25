import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.FlatSpec
import util.CirceMarshalling._
import util.TestUtils._

import scala.concurrent.duration.FiniteDuration

class CirceMarshallingSpec extends FlatSpec {
  implicit val system = ActorSystem("FlatSpec")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = materializer.executionContext

  val instance = ClassWithEncoderAndDecoder("text", 89, boolean = false)

  "Unmarshalling" should "work for every class with decoder" in {
    val entity = HttpEntity(ContentTypes.`application/json`, instance.asJson.noSpaces)
    val request = HttpRequest(HttpMethods.POST, entity = entity)
    val result = fromMessageUnmarshaller.apply(request)
    assert(await(result) == instance)
  }

  "Marshalling" should "work for every class with encoder" in {
    val response = await(Marshal(instance).to[HttpResponse])
    assert(response.status == StatusCodes.OK)
    assert(response.entity.contentType == ContentTypes.`application/json`)
    val strict = await(response.entity.toStrict(FiniteDuration(10, TimeUnit.SECONDS)))
    val content = strict.data.utf8String
    val json = parse(content)
    assert(json.isRight)
    val decoded = json.right.get.as[ClassWithEncoderAndDecoder]
    assert(decoded.contains(instance))
  }
}
