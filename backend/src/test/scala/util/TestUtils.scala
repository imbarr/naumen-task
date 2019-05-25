package util

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object TestUtils {
  case class ClassWithEncoderAndDecoder(string: String, int: Int, boolean: Boolean)
  implicit val classEncoder = deriveEncoder[ClassWithEncoderAndDecoder]
  implicit val classDecoder = deriveDecoder[ClassWithEncoderAndDecoder]

  def await[T](future: Future[T]): T =
    Await.result(future, Duration.Inf)

  def longRunning(implicit executionContext: ExecutionContext): Future[Nothing] = Future {
    Thread.sleep(1000)
    throw new Exception()
  }
}
