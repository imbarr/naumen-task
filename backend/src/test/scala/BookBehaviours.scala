import data.{BookEntry, BookEntryWithId, Phone}
import org.scalatest.{BeforeAndAfter, FlatSpec, OptionValues}
import storage.Book
import util.TestData.{entries, entry, phone}
import util.TestUtils._

abstract class BookBehaviours extends FlatSpec with BeforeAndAfter with OptionValues {

  var book: Book
  var added: List[BookEntryWithId] = _

  before {
    added = addEntries(entries)
  }

  def anyBook(): Unit = {
    it should "add all entries" in {
      val all = await(book.getEntries()).toSet
      assert(added.toSet subsetOf all)
    }

    it should "not add duplicates" in {
      val result = await(book.add(entry))
      assert(result.isEmpty)
    }

    it should "get range of entries" in {
      val all = await(book.getEntries()).toSet
      val single = await(book.getEntries(range = Some((0, 0))))
      assert(single.size == 1)
      val entireRange = await(book.getEntries(range = Some((0, all.size - 1))))
      assert(entireRange.toSet == all)
      val overflow = await(book.getEntries(range = Some((0, all.size + 22))))
      assert(overflow.toSet == all)
      val middle = all.size / 2
      val firstHalf = await(book.getEntries(range = Some((0, middle)))).toSet
      val secondHalf = await(book.getEntries(range = Some((middle + 1, all.size - 1)))).toSet
      assert((firstHalf intersect secondHalf) == Set.empty)
      assert((firstHalf union secondHalf) == all)
    }

    it should "get number of entries" in {
      val expected = await(book.getEntries()).size
      val actual = await(book.getSize())
      assert(actual == expected)
    }

    it should "get entry by id" in {
      val entry = await(book.getEntries()).headOption.value
      val expected = BookEntry(entry.name, entry.phone)
      val byId = await(book.getById(entry.id))
      assert(byId.contains(expected))
    }

    it should "throw exception on invalid range parameters" in {
      val ranges = List((-2, 3), (10, 9), (-2, -3))
      for(range <- ranges) {
        assertThrows[IllegalArgumentException]{
          await(book.getEntries(range = Some(range)))
        }
      }
    }

    it should "assign different ids" in {
      val ids = added.map(_.id).toSet
      assert(ids.size == entries.length)
    }

    it should "change phone number" in changeEntriesTest { entry =>
      val changedEntry = BookEntryWithId(entry.id, entry.name, phone)
      val success = await(book.changePhoneNumber(entry.id, phone))
      assert(success)
      Set(changedEntry)
    }

    it should "change name" in changeEntriesTest { entry =>
      val newName = entry.name + "0"
      val changedEntry = BookEntryWithId(entry.id, newName, entry.phone)
      val success = await(book.changeName(entry.id, newName))
      assert(success)
      Set(changedEntry)
    }

    it should "replace entry" in changeEntriesTest { entry =>
      val newName = entry.name + "0"
      val changedEntry = BookEntryWithId(entry.id, newName, phone)
      val success = await(book.replace(entry.id, newName, phone))
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
        val expected = await(book.getEntries()).filter(_.name.contains(substring))
        val actual = await(book.getEntries(nameSubstring = Some(substring)))
        assert(expected.toSet == actual.toSet)
      }
    }

    it should "find elements by telephone substring" in {
      for (substring <- List("800--555", "+7", "000000")) {
        val plain = Phone.withoutDelimiters(substring)
        val expected = await(book.getEntries()).filter(_.phone.withoutDelimiters.contains(plain))
        val actual = await(book.getEntries(phoneSubstring = Some(substring)))
        assert(actual.toSet == expected.toSet)
      }
    }

    it should "not change state if entry does not exist" in {
      val old = await(book.getEntries())
      val id = old.map(_.id).max + 1
      assert(!await(book.changePhoneNumber(id, phone)))
      assert(!await(book.changeName(id, "")))
      assert(!await(book.replace(id, "", phone)))
      assert(!await(book.remove(id)))
      assert(old.toSet == await(book.getEntries()).toSet)
    }
  }

  def changeEntriesTest(operation: BookEntryWithId => Set[BookEntryWithId]): Unit = {
    val oldEntries = added.toSet
    val entry = oldEntries.headOption.value
    val changedEntries = operation(entry)
    val newEntries = await(book.getEntries()).toSet
    assert(changedEntries subsetOf (newEntries diff oldEntries))
    assert((oldEntries diff newEntries) == Set(entry))
  }

  def addEntries(entries: List[BookEntry]): List[BookEntryWithId] =
    entries.map(entry => {
      val id = await(book.add(entry)).value
      BookEntryWithId(id, entry.name, entry.phone)
    })
}
