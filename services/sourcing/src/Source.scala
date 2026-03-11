package news.sipyr.sourcing

final case class Source(
    id: Source.ID,
    location: Source.Location,
    articles: List[Article]
)

object Source {
  opaque type ID = String

  object ID {
    def fromString(value: String): ID = value
  }

  enum Location {
    case RSS(url: String)
  }
}
