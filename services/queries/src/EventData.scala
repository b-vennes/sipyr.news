package news.sipyr.queries

import io.circe.Json

final case class EventData(
  typeName: EventData.TypeName,
  index: EventData.Index,
  content: EventData.Content)

object EventData {
  opaque type TypeName = String
  opaque type Index = Integer
  opaque type Content = Json

  object TypeName {
    @inline def fromString(value: String): TypeName = value
  }

  object Index {
    @inline def fromInt(value: Int): Index = value
  }

  object Content {
    @inline def fromJson(value: Json): Content = value
  }
}
