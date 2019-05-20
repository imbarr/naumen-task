import org.scalatest.FlatSpec

import scala.concurrent.{ExecutionContext, Future}

class TaskManagerSpec extends FlatSpec {
  implicit val executionContext = ExecutionContext.global

  "TaskManager" should "add and return tasks" in {
    val manager = new TaskManager()
    val first = Future.successful()
    val second = Future.failed(new Exception)
    val firstId = manager.add(first)
    val secondId = manager.add(second)
    assert(first != second)
    assert(manager.get(firstId).contains(first))
    assert(manager.get(secondId).contains(second))
  }

  it should "return number of tasks in progress" in {
    val manager = new TaskManager()
    val tasks = List(
      Future(Thread.sleep(1000)),
      Future(Thread.sleep(1000)),
      Future.successful(),
      Future.failed(new Exception),
      Future.failed(new Exception)
    )
    for (task <- tasks)
      manager.add(task)
    assert(manager.count == 2)
  }
}
