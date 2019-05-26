import java.nio.file.{Files, Path}

import com.google.common.jimfs.{Configuration, Jimfs}
import filesystem.DataSaver
import io.circe.parser.parse
import org.scalatest.{BeforeAndAfter, EitherValues, FlatSpec}
import util.TestUtils._

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.ExecutionContext


class DataSaverSpec extends FlatSpec with BeforeAndAfter with EitherValues {
  implicit val executionContext = ExecutionContext.global

  "DataSaver" should "save file with correct name" in dataSaverTest { (directory, dataSaver) =>
    val data = ClassWithEncoderAndDecoder("string", 0, boolean = false)
    await(dataSaver.save("something", data))
    val files = Files.list(directory).iterator().asScala.toList
    val names = files.map(_.getFileName.toString)
    assert(names == List("something_1.json"))
    val bytes = Files.readAllBytes(files.head)
    val parsed = parse(new String(bytes))
    val actualData = parsed.right.value.as[ClassWithEncoderAndDecoder]
    assert(actualData.contains(data))
  }

  it should "chose maximum number" in dataSaverTest { (directory, dataSaver) =>
    val data = ClassWithEncoderAndDecoder("string", 0, boolean = false)
    val newFiles = List(
      "name_22.json",
      "name_34.xml",
      "name_18.json",
      "names_40.json",
      "name50.json",
      "name_name.json"
    )
    for (file <- newFiles)
      Files.createFile(directory.resolve(file))
    await(dataSaver.save("name", data))
    val files = Files.list(directory).iterator().asScala.toList
    val names = files.map(_.getFileName.toString)
    val expected = "name_23.json" +: newFiles
    assert(names.toSet == expected.toSet)
  }

  private def dataSaverTest(test: (Path, DataSaver) => Unit): Unit = {
    val fileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform)
    val directory = fileSystem.getRootDirectories.iterator().next().resolve("work")
    val dataSaver = new DataSaver(directory)
    test(directory, dataSaver)
  }
}
