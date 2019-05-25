import java.nio.file.{FileSystem, Files, Path}

import com.google.common.jimfs.{Configuration, Jimfs}
import filesystem.DataSaver
import io.circe.parser.parse
import org.scalatest.{BeforeAndAfter, FlatSpec}
import util.TestUtils._

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.ExecutionContext


class DataSaverSpec extends FlatSpec with BeforeAndAfter {
  implicit val executionContext = ExecutionContext.global

  var fileSystem: FileSystem = _
  var directory: Path = _
  var dataSaver: DataSaver = _

  before {
    fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform)
    directory = fileSystem.getRootDirectories.iterator().next().resolve("work")
    dataSaver = new DataSaver(directory)
  }

  "filesystem.DataSaver" should "save file with correct name" in {
    val data = ClassWithEncoderAndDecoder("строка", 0, boolean = false)
    await(dataSaver.save("something", data))
    val files = Files.list(directory).iterator().asScala.toList
    val names = files.map(_.getFileName.toString)
    assert(names == List("something_1.json"))
    val bytes = Files.readAllBytes(files.head)
    val parsed = parse(new String(bytes))
    assert(parsed.isRight)
    val actualData = parsed.right.get.as[ClassWithEncoderAndDecoder]
    assert(actualData.contains(data))
  }

  it should "chose maximum number" in {
    val data = ClassWithEncoderAndDecoder("строка", 0, boolean = false)
    val files = List(
      "name_22.json",
      "name_34.xml",
      "name_18.json",
      "names_40.json",
      "name50.json",
      "name_name.json"
    )
    for (file <- files)
      Files.createFile(directory.resolve(file))
    await(dataSaver.save("name", data))
    val expected = "name_23.json" +: files
    assert(getFileNames.toSet == expected.toSet)
  }

  def getFileNames: List[String] = {
    val files = Files.list(directory).iterator().asScala.toList
    files.map(_.getFileName.toString)
  }
}
