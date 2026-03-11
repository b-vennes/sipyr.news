package news.sipyr.eventstore

import news.sipyr.events.{SourceEvent, SourceInitialized, ArticlesAdded}

import cats.implicits._

object SourceEventExt {
  val initializedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("initialized")
  val articlesAddedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("articlesAdded")

  def decode(eventData: EventData): Option[SourceEvent] =
    eventData.eventTypeName match {
      case t if t === initializedEventType =>
        eventData.content.toJson
          .as[SourceInitialized]
          .toOption
          .map(SourceEvent.initialized)
      case t if t === articlesAddedEventType =>
        eventData.content.toJson
          .as[ArticlesAdded]
          .toOption
          .map(SourceEvent.articlesAdded)
      case _ => None
    }
}
