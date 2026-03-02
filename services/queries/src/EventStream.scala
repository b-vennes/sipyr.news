package news.sipyr.queries

final case class EventStream(
  id: EventStream.ID,
  category: EventStream.Category
)

object EventStream {
  opaque type Category = String
  opaque type ID = String

  object ID {
    @inline def fromString(value: String): ID = value
  }

  object Categories {
    val feeds: Category = "feeds"
    val sources: Category = "sources"
  }
}
