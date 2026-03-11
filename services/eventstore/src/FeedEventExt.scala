package news.sipyr.eventstore

import news.sipyr.events.{
  FeedEvent,
  FeedCreated,
  SourcesAdded,
  SourcesRemoved,
  MaintainerChanged
}

import cats.implicits._

object FeedEventExt {
  val createdEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("created")
  val sourcesAddedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("sourcesAdded")
  val sourcesRemovedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("sourcesRemoved")
  val maintainerChangedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("maintainerChanged")

  def decode(eventData: EventData): Option[FeedEvent] =
    eventData.eventTypeName match {
      case t if t === createdEventType =>
        eventData.content.toJson
          .as[FeedCreated]
          .toOption
          .map(FeedEvent.created)
      case t if t === sourcesAddedEventType =>
        eventData.content.toJson
          .as[SourcesAdded]
          .toOption
          .map(FeedEvent.sourcesAdded)
      case t if t === sourcesRemovedEventType =>
        eventData.content.toJson
          .as[SourcesRemoved]
          .toOption
          .map(FeedEvent.sourcesRemoved)
      case t if t === maintainerChangedEventType =>
        eventData.content.toJson
          .as[MaintainerChanged]
          .toOption
          .map(FeedEvent.maintainerChanged)
      case _ => None
    }
}
