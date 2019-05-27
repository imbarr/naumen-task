package util

import data.{BookEntry, BookEntryWithId, Phone}

object TestData {
  val entryWithId = BookEntryWithId(22, "John", Phone.fromString("+7 800 5553535").right.get)
  val entry = BookEntry("John Doe", Phone.fromString("+78005553535").right.get)
  val phone = Phone.fromString("+39 06 698577777").right.get

  val entries = entry +: List(
    BookEntry("Jane Doe", Phone.fromString("+79223101010").right.get),
    BookEntry("Jane Doe", Phone.fromString("+79273101010").right.get),
    BookEntry("Someone Else", Phone.fromString("+79223101010").right.get)
  )
  val anotherEntries = entries.map(entry => BookEntry(entry.name + "!", entry.phone))
  val entriesWithIds = entryWithId +: List(
    BookEntryWithId(11, "Boris", Phone.fromString("+78005553535").right.get),
    BookEntryWithId(121, "Victor", Phone.fromString("+79228222201").right.get)
  )
}
