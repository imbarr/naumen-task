package filesystem

import java.nio.file.{Files, Path}

import io.circe.Encoder
import io.circe.syntax.EncoderOps

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DataSaver(path: Path)(implicit executionContext: ExecutionContext) {
  def save[A: Encoder](name: String, data: A): Future[Unit] = Future {
    Files.createDirectories(path)
    val next = getNextNumber(name)
    val newFilePath = path.resolve(s"${name}_$next.json")
    val writer = Files.newBufferedWriter(newFilePath)
    writer.write(data.asJson.spaces2)
    writer.close()
  }

  private def getNextNumber(prefix: String): Int = {
    val files = Files.list(path).iterator().asScala
    val filenames = files.map(_.getFileName.toString)
    val numbers = filenames.flatMap(filename => getNumber(filename, prefix))
    numbers.fold(0)(_ max _) + 1
  }

  private def getNumber(filename: String, prefix: String): Option[Int] = {
    val regex = s"""^${prefix}_(.*).json""".r
    filename match {
      case regex(number) => Try(number.toInt).toOption
      case _ => None
    }
  }
}
