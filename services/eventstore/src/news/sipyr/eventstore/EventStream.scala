package news.sipyr.eventstore

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

  object Category {
    @inline def fromString(value: String): Category = value
  }

  object Categories {
    val feeds: Category = "feeds"
    val sources: Category = "sources"
  }

  extension (id: ID) {
    @inline def streamName: String = id
  }

  extension (category: Category) {
    @inline def typeName: String = category
  }
}
