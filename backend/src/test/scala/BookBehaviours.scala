import data.{BookEntry, BookEntryWithId}
import org.scalatest.{Assertion, BeforeAndAfter, FlatSpec}
import storage.Book

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class BookBehaviours extends FlatSpec with BeforeAndAfter {
  val entries = List(
    BookEntry("John Doe", "88005553535"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Someone Else", "dfgdgdgdfgd")
  )

  var book: Book
  var added: List[BookEntryWithId] = _

  before {
    added = addEntries(entries)
  }

  def anyBook(): Unit = {
    it should "add all entries" in {
      val all = await(book.get()).toSet
      assert(added.toSet subsetOf all)
    }

    it should "get range of entries" in {
      val all = await(book.get()).toSet
      val single = await(book.get(range = Some((0, 0))))
      assert(single.size == 1)
      val entireRange = await(book.get(range = Some((0, all.size - 1))))
      assert(entireRange.toSet == all)
      val overflow = await(book.get(range = Some((0, all.size + 22))))
      assert(overflow.toSet == all)
      val middle = all.size / 2
      val firstHalf = await(book.get(range = Some((0, middle)))).toSet
      val secondHalf = await(book.get(range = Some((middle + 1, all.size - 1)))).toSet
      assert((firstHalf intersect secondHalf) == Set.empty)
      assert((firstHalf union secondHalf) == all)
    }

    it should "get number of entries" in {
      val expected = await(book.get()).size
      val actual = await(book.getSize())
      assert(actual == expected)
    }

    it should "get entry by id" in {
      val entry = await(book.get()).head
      val expected = BookEntry(entry.name, entry.phoneNumber)
      val byId = await(book.getById(entry.id))
      assert(byId.contains(expected))
    }

    it should "throw exception on invalid range parameters" in {
      val ranges = List((-2, 3), (10, 9), (-2, -3))
      for(range <- ranges) {
        assertThrows[IllegalArgumentException]{
          await(book.get(range = Some(range)))
        }
      }
    }

    it should "assign different ids" in {
      val ids = added.map(_.id).toSet
      assert(ids.size == entries.length)
    }

    it should "change phone number" in changeEntriesTest { entry =>
      val newPhoneNumber = entry.phoneNumber + "0"
      val changedEntry = BookEntryWithId(entry.id, entry.name, newPhoneNumber)
      val success = await(book.changePhoneNumber(entry.id, newPhoneNumber))
      assert(success)
      Set(changedEntry)
    }

    it should "change name" in changeEntriesTest { entry =>
      val newName = entry.name + "0"
      val changedEntry = BookEntryWithId(entry.id, newName, entry.phoneNumber)
      val success = await(book.changeName(entry.id, newName))
      assert(success)
      Set(changedEntry)
    }

    it should "replace entry" in changeEntriesTest { entry =>
      val newName = entry.name + "0"
      val newPhoneNumber = entry.phoneNumber + "0"
      val changedEntry = BookEntryWithId(entry.id, newName, newPhoneNumber)
      val success = await(book.replace(entry.id, newName, newPhoneNumber))
      assert(success)
      Set(changedEntry)
    }

    it should "remove entry" in changeEntriesTest { entry =>
      val success = await(book.remove(entry.id))
      assert(success)
      Set.empty
    }

    it should "find elements by name substring" in {
      for (substring <- List("a", "Jane", "Doe")) {
        val expected = await(book.get()).filter(_.name.contains(substring))
        val actual = await(book.get(nameSubstring = Some(substring)))
        assert(expected.toSet == actual.toSet)
      }
    }

    it should "find elements by telephone substring" in {
      for (substring <- List("800", "+7", "000000")) {
        val expected = await(book.get()).filter(_.phoneNumber.contains(substring))
        val actual = await(book.get(phoneSubstring = Some(substring)))
        assert(expected.toSet == actual.toSet)
      }
    }

    it should "not change state if entry does not exist" in {
      val old = await(book.get())
      val id = old.map(_.id).max + 1
      assert(!await(book.changePhoneNumber(id, "")))
      assert(!await(book.changeName(id, "")))
      assert(!await(book.replace(id, "", "")))
      assert(!await(book.remove(id)))
      assert(old.toSet == await(book.get()).toSet)
    }
  }

  def changeEntriesTest(operation: BookEntryWithId => Set[BookEntryWithId]): Assertion = {
    val oldEntries = added.toSet
    val entry = oldEntries.head
    val changedEntries = operation(entry)
    val newEntries = await(book.get()).toSet
    assert(changedEntries subsetOf (newEntries diff oldEntries))
    assert((oldEntries diff newEntries) == Set(entry))
  }

  def addEntries(entries: List[BookEntry]): List[BookEntryWithId] =
    entries.map(entry => {
      val id = await(book.add(entry))
      BookEntryWithId(id, entry.name, entry.phoneNumber)
    })

  def await[T](future: Future[T]): T =
    Await.result(future, Duration.Inf)
}
