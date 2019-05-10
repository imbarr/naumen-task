package util

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromMessageUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import akka.util.ByteString
import io.circe.syntax.EncoderOps
import io.circe._

import scala.concurrent.Future

object CirceMarshalling {
  implicit def fromMessageUnmarshaller[A: Decoder]: FromMessageUnmarshaller[A] =
    Unmarshaller.withMaterializer[HttpMessage, ByteString](_ => implicit materializer => message =>
      entityToBytes(message.entity))
      .map(bytesToJson)
      .map(jsonToObject[A])

  implicit def toResponseMarshaller[A: Encoder](implicit p: Printer = Printer.noSpaces): ToResponseMarshaller[A] =
    toEntityMarshaller.map(entity => HttpResponse(StatusCodes.OK, entity = entity))

  implicit def toEntityMarshaller[A: Encoder](implicit p: Printer = Printer.noSpaces): ToEntityMarshaller[A] =
    toJsonMarshaller.compose(_.asJson)

  private def entityToBytes(entity: HttpEntity)(implicit m: Materializer): Future[ByteString] = {
    if (entity.contentType != ContentTypes.`application/json`)
      throw Unmarshaller.UnsupportedContentTypeException(ContentTypes.`application/json`)
    entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
  }

  private def bytesToJson(bytes: ByteString): Json = {
    if (bytes == ByteString.empty)
      throw Unmarshaller.NoContentException
    jawn.parseByteBuffer(bytes.asByteBuffer).fold(throw _, identity)
  }

  private def jsonToObject[A: Decoder](json: Json): A =
    json.as[A].fold(throw _, identity)

  private def toJsonMarshaller(implicit printer: Printer): Marshaller[Json, RequestEntity] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { json =>
      HttpEntity(ContentTypes.`application/json`, ByteString(printer.prettyByteBuffer(json)))
    }
}