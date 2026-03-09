package news.sipyr.queries

import news.sipyr.events.{
  FeedCreated,
  FeedEvent,
  FeedID,
  MaintainerChanged,
  MaintainerID,
  SourceID,
  SourceIDs,
  SourcesAdded,
  SourcesRemoved
}

import cats.data.Chain
import io.circe.Decoder

object SourceIDsExt {
  private given Decoder[FeedID] = Decoder[Long].map(FeedID(_))
  private given Decoder[MaintainerID] = Decoder[Long].map(MaintainerID(_))
  private given Decoder[SourceID] = Decoder[Long].map(SourceID(_))

  private given Decoder[FeedCreated] =
    Decoder.forProduct4("id", "maintainer", "name", "sources")(FeedCreated(_, _, _, _))
  private given Decoder[SourcesAdded] =
    Decoder.forProduct1("sources")(SourcesAdded(_))
  private given Decoder[SourcesRemoved] =
    Decoder.forProduct1("sources")(SourcesRemoved(_))
  private given Decoder[MaintainerChanged] =
    Decoder.forProduct1("maintainer")(MaintainerChanged(_))

  private val createdEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("created")
  private val sourcesAddedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("sourcesAdded")
  private val sourcesRemovedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("sourcesRemoved")
  private val maintainerChangedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("maintainerChanged")

  def hydrate(events: Chain[EventData]): SourceIDs =
    events.foldLeft(SourceIDs(List.empty)) { (aggregate, eventData) =>
      decode(eventData).fold(aggregate)(event => apply(aggregate, event))
    }

  private def decode(eventData: EventData): Option[FeedEvent] =
    if (eventData.eventTypeName == createdEventType)
      eventData.content.toJson
        .as[FeedCreated]
        .toOption
        .map(FeedEvent.created)
    else if (eventData.eventTypeName == sourcesAddedEventType)
      eventData.content.toJson
        .as[SourcesAdded]
        .toOption
        .map(FeedEvent.sourcesAdded)
    else if (eventData.eventTypeName == sourcesRemovedEventType)
      eventData.content.toJson
        .as[SourcesRemoved]
        .toOption
        .map(FeedEvent.sourcesRemoved)
    else if (eventData.eventTypeName == maintainerChangedEventType)
      eventData.content.toJson
        .as[MaintainerChanged]
        .toOption
        .map(FeedEvent.maintainerChanged)
    else None

  private def apply(aggregate: SourceIDs, event: FeedEvent): SourceIDs =
    event match {
      case FeedEvent.CreatedCase(created) =>
        SourceIDs(created.sources)
      case FeedEvent.SourcesAddedCase(added) =>
        SourceIDs(
          added.sources.foldLeft(aggregate.value) { (sources, source) =>
            if (sources.contains(source)) sources
            else sources :+ source
          }
        )
      case FeedEvent.SourcesRemovedCase(removed) =>
        val removedSources = removed.sources.toSet
        SourceIDs(aggregate.value.filterNot(source => removedSources.contains(source)))
      case FeedEvent.MaintainerChangedCase(_) =>
        aggregate
    }
}
