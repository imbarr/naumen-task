import storage.{Book, InMemoryBook}

class InMemoryBookSpec extends BookBehaviours {
  override var book: Book = new InMemoryBook()

  after {
    book = new InMemoryBook()
  }

  "InMemoryBook" should behave like anyBook
}
