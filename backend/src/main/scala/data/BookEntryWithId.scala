package data

case class BookEntryWithId(id: Int, name: String, phone: Phone) {
  def withoutId: BookEntry =
    BookEntry(name, phone)

  def this(id: Int, bookEntry: BookEntry) =
    this(id, bookEntry.name, bookEntry.phone)
}