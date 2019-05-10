import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import org.scalatest.FlatSpec
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import util.CirceMarshalling._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import io.circe.parser._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class CirceMarshallingSpec extends FlatSpec {
  implicit val system = ActorSystem("FlatSpec")
  implicit val materializer = ActorMaterializer()

  case class Class(string: String, int: Int, boolean: Boolean)

  implicit val decoder = deriveDecoder[Class]
  implicit val encoder = deriveEncoder[Class]

  val instance = Class("text", 89, boolean = false)

  "fromRequestUnmarshaller" should "work for every class with decoder" in {
    val entity = HttpEntity(ContentTypes.`application/json`, instance.asJson.noSpaces)
    val request = HttpRequest(HttpMethods.POST, entity = entity)
    val result = Await.ready(fromRequestUnmarshaller.apply(request), Duration.Inf).value.get
    assert(result.toOption.contains(instance))
  }

  "toResponseMarshaller" should "work for every class with encoder" in {
    val result = Await.ready(Marshal(instance).to(toResponseMarshaller, global), Duration.Inf).value.get.get
    assert(result.status == StatusCodes.OK)
    assert(result.entity.contentType == ContentTypes.`application/json`)
    val string = Await.ready(Unmarshal(result.entity).to[String], Duration.Inf).value.get.get
    val json = parse(string).right.get
    assert(json.as[Class].contains(instance))
  }
}
