package news.sipyr.queries

import io.circe.Json

final case class EventData(
    persistedAt: EventData.PersistedAt,
    typeName: EventData.TypeName,
    streamName: EventData.StreamName,
    index: EventData.Index,
    eventTypeName: EventData.EventTypeName,
    content: EventData.Content
)

object EventData {
  opaque type PersistedAt = EpochSeconds
  opaque type TypeName = String
  opaque type StreamName = String
  opaque type Index = Integer
  opaque type EventTypeName = String
  opaque type Content = Json

  object TypeName {
    @inline def fromString(value: String): TypeName = value
  }

  object StreamName {
    @inline def fromString(value: String): StreamName = value
  }

  object Index {
    @inline def fromInt(value: Int): Index = value
  }

  object Content {
    @inline def fromJson(value: Json): Content = value
  }

  object PersistedAt {
    @inline def fromEpochSeconds(value: EpochSeconds): PersistedAt = value
  }

  object EventTypeName {
    @inline def fromString(value: String): EventTypeName = value
  }
}
