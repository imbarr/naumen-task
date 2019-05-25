package server

import scala.concurrent.Future

class TaskManager(maximumNumberOfTasks: Int) {
  private var tasks = Map[Int, Future[Unit]]()
  private var lastId = 0

  def add(task: => Future[Unit]): Option[Int] = {
    synchronized {
      if (isFull) {
        None
      }
      else {
        lastId += 1
        tasks += lastId -> task
        Some(lastId)
      }
    }
  }

  def get(id: Int): Option[Future[Unit]] =
    tasks.get(id)

  private def isFull: Boolean = {
    val runningCount = tasks.values.count(!_.isCompleted)
    runningCount >= maximumNumberOfTasks
  }
}
