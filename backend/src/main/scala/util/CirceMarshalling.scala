package util
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, FromResponseUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.util.ByteString
import io.circe._

import scala.concurrent.Future

object CirceMarshalling {
  implicit def fromRequestUnmarshaller[A: Decoder]: FromRequestUnmarshaller[A] =
    Unmarshaller.withMaterializer[HttpRequest, ByteString](_ => implicit materializer => request =>
        entityToBytes(request.entity))
      .map(bytesToJson)
      .map(jsonToObject[A])

  implicit def fromResponseUnmarshaller[A: Decoder]: FromResponseUnmarshaller[A] =
    Unmarshaller.withMaterializer[HttpResponse, ByteString](_ => implicit materializer => request =>
      entityToBytes(request.entity))
      .map(bytesToJson)
      .map(jsonToObject[A])

  private def entityToBytes(entity: HttpEntity)(implicit m: Materializer): Future[ByteString] = {
    if (entity.contentType != ContentTypes.`application/json`)
      throw Unmarshaller.UnsupportedContentTypeException(ContentTypes.`application/json`)
    entity match {
      case HttpEntity.Strict(_, data) => FastFuture.successful(data)
      case _ => entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    }
  }

  private def bytesToJson(bytes: ByteString): Json = {
    if (bytes == ByteString.empty)
      throw Unmarshaller.NoContentException
    jawn.parseByteBuffer(bytes.asByteBuffer).fold(throw _, identity)
  }

  private def jsonToObject[A: Decoder](json: Json): A =
    Decoder[A].decodeJson(json).fold(throw _, identity)

//  implicit def emptyMarshaller: ToResponseMarshaller[Unit.type] =
//    Marshaller.withFixedContentType(ContentTypes.NoContentType) { _ => HttpResponse()}

  implicit def toResponseMarshaller[A: Encoder](implicit printer: Printer = Printer.noSpaces): ToResponseMarshaller[A] =
    ToEntityMarshaller.map(entity => HttpResponse(StatusCodes.OK, entity = entity))

  implicit def ToEntityMarshaller[A: Encoder](implicit printer: Printer = Printer.noSpaces): Marshaller[A, RequestEntity] =
    jsonMarshaller.compose(Encoder[A].apply)

  private def jsonMarshaller(implicit printer: Printer = Printer.noSpaces): Marshaller[Json, RequestEntity] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { json =>
      HttpEntity(ContentTypes.`application/json`, ByteString(printer.prettyByteBuffer(json)))
    }
}