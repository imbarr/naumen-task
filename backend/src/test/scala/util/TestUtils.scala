package util

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object TestUtils {
  case class Class(string: String, int: Int, boolean: Boolean)
  implicit val encoder = deriveEncoder[Class]
  implicit val decoder = deriveDecoder[Class]

  def await[T](future: Future[T]): T =
    Await.result(future, Duration.Inf)

  def longRunning(implicit executionContext: ExecutionContext): Future[Nothing] = Future {
    Thread.sleep(1000)
    throw new Exception()
  }
}
