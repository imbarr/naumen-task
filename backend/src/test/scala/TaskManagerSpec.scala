import org.scalatest.FlatSpec
import server.TaskManager
import util.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class TaskManagerSpec extends FlatSpec {
  implicit val executionContext = ExecutionContext.global

  "server.TaskManager" should "add and return tasks" in {
    val manager = new TaskManager(10)
    val first = Future.successful()
    val second = Future.failed(new Exception)
    val firstId = manager.add(first)
    val secondId = manager.add(second)
    assert(firstId.isDefined)
    assert(secondId.isDefined)
    assert(first != second)
    assert(manager.get(firstId.get).contains(first))
    assert(manager.get(secondId.get).contains(second))
  }

  it should "not add task if it is full" in {
    val manager = new TaskManager(1)
    val firstId = manager.add(longRunning)
    assert(firstId.isDefined)
    val secondId = manager.add(Future.successful())
    assert(secondId.isEmpty)
  }

  it should "be synchronized" in {
    val manager = new TaskManager(1)
    val firstIdFuture = Future {
      manager.add(getTaskWithDelay)
    }
    val secondIdFuture = Future {
      manager.add(getTaskWithDelay)
    }
    val firstAdded = await(firstIdFuture).isDefined
    val secondAdded = await(secondIdFuture).isDefined
    assert((firstAdded && !secondAdded) || (!firstAdded && secondAdded))
  }

  def getTaskWithDelay: Future[Unit] = {
    Thread.sleep(100)
    longRunning
  }
}
