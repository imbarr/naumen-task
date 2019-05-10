import data.{BookEntry, BookEntryWithId}
import org.scalatest._
import storage.InMemoryBook

class InMemoryBookSpec extends FlatSpec with Matchers with BeforeAndAfter {
  val entries = List(
    BookEntry("John Doe", "88005553535"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Jane Doe", "+79223101010"),
    BookEntry("Someone Else", "dfgdgdgdfgd")
  )

  var book: InMemoryBook = _

  before {
    book = new InMemoryBook()
  }

  "InMemoryBook" should "be empty at start" in {
    assert(book.getAll == Nil)
  }

  it should "add all entries" in {
    assert(addEntries(entries) == book.getAll)
  }

  it should "assign different ids" in {
    val ids = addEntries(entries).map(_.id).toSet
    assert(ids.size == entries.length)
  }

  it should "change phone number" in changeEntriesTest { entry =>
    val newPhoneNumber = entry.phoneNumber + "0"
    val changedEntry = BookEntryWithId(entry.id, entry.name, newPhoneNumber)
    val success = book.changePhoneNumber(entry.id, newPhoneNumber)
    assert(success)
    Set(changedEntry)
  }

  it should "change name" in changeEntriesTest { entry =>
    val newName = entry.name + "0"
    val changedEntry = BookEntryWithId(entry.id, newName, entry.phoneNumber)
    val success = book.changeName(entry.id, newName)
    assert(success)
    Set(changedEntry)
  }

  it should "replace entry" in changeEntriesTest { entry =>
    val newName = entry.name + "0"
    val newPhoneNumber = entry.phoneNumber + "0"
    val changedEntry = BookEntryWithId(entry.id, newName, newPhoneNumber)
    val success = book.replace(entry.id, newName, newPhoneNumber)
    assert(success)
    Set(changedEntry)
  }

  it should "remove entry" in changeEntriesTest { entry =>
    val success = book.remove(entry.id)
    assert(success)
    Set.empty
  }

  it should "find elements by name substring" in {
    addEntries(entries)
    assert(book.findByNameSubstring("").length == entries.length)
    assert(book.findByNameSubstring("Jane").length == 2)
    assert(book.findByNameSubstring("Definitely not a name").isEmpty)
  }

  it should "find elements by telephone substring" in {
    addEntries(entries)
    assert(book.findByPhoneNumberSubstring("").length == entries.length)
    assert(book.findByPhoneNumberSubstring("+7").length == 2)
    assert(book.findByPhoneNumberSubstring("Definitely not a phone number").isEmpty)
  }

  it should "not change state if entry does not exist" in {
    val added = addEntries(entries)
    val id = added.map(_.id).max + 1
    assert(!book.changePhoneNumber(id, ""))
    assert(!book.changeName(id, ""))
    assert(!book.replace(id, "", ""))
    assert(!book.remove(id))
    assert(added.toSet == book.getAll.toSet)
  }

  private def changeEntriesTest(operation: BookEntryWithId => Set[BookEntryWithId]): Assertion = {
    val oldEntries = addEntries(entries).toSet
    val entry = oldEntries.head
    val changedEntries = operation(entry)
    val newEntries = book.getAll.toSet
    assert((newEntries diff oldEntries) == changedEntries)
    assert((oldEntries diff newEntries) == Set(entry))
  }

  private def addEntries(entries: List[BookEntry]): List[BookEntryWithId] =
    entries.map(entry => {
      val id = book.add(entry)
      BookEntryWithId(id, entry.name, entry.phoneNumber)
    })
}
