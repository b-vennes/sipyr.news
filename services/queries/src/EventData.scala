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
}
