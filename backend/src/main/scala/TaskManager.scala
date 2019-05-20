import scala.concurrent.Future

class TaskManager {
  private var tasks = Map[Int, Future[Unit]]()
  private var lastId = 0

  def add(task: Future[Unit]): Int = {
    lastId += 1
    tasks += lastId -> task
    lastId
  }

  def get(id: Int): Option[Future[Unit]] =
    tasks.get(id)

  def count: Int =
    tasks.values.count(!_.isCompleted)
}
