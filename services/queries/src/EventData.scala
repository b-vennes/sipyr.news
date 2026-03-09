package news.sipyr.queries

import io.circe.Json
import cats.implicits._
import cats.kernel.Eq

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

    given Eq[EventTypeName] = Eq.by[EventTypeName, String](a => a)
  }

  extension (persistedAt: PersistedAt) {
    @inline def toEpochSeconds: EpochSeconds = persistedAt
  }

  extension (typeName: TypeName) {
    @inline def toTypeName: String = typeName
  }

  extension (streamName: StreamName) {
    @inline def toStreamName: String = streamName
  }

  extension (index: Index) {
    @inline def toIndex: Int = index
  }

  extension (eventTypeName: EventTypeName) {
    @inline def toEventTypeName: String = eventTypeName
  }

  extension (content: Content) {
    @inline def toJson: Json = content
  }
}
