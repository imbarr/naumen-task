package storage.database

import java.io.Closeable

import com.typesafe.scalalogging.Logger
import slick.jdbc.SQLServerProfile.api._
import storage.database.Functions._
import storage.database.tables.PhoneNumbers

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EntriesCleaner(database: Database, lifespanInMillis: Long, intervalInMillis: Long)
                    (implicit log: Logger, context: ExecutionContext) extends Closeable {

  private val thread = new Thread(() => deleteUntilInterrupted())
  thread.start()

  override def close(): Unit =
    thread.interrupt()

  @tailrec
  private def deleteUntilInterrupted(): Unit = {
    logResult(deleteOld())
    try {
      Thread.sleep(intervalInMillis)
    } catch {
      case _: InterruptedException => return
    }
    deleteUntilInterrupted()
  }

  private def deleteOld(): Future[Int] = {
    val query = TableQuery[PhoneNumbers].filter(_.created < addMillis(-lifespanInMillis, now)).delete
    database.run(query)
  }

  private def logResult(future: Future[Int]): Unit = {
    future.onComplete {
      case Success(number) => log.debug(s"Deleted $number expired entries")
      case Failure(error) => log.error("Failed to perform cleanup of entries", error)
    }
  }
}
