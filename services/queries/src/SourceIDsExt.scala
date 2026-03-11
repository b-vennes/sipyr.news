package news.sipyr.queries

import news.sipyr.events.{
  FeedCreated,
  FeedEvent,
  MaintainerChanged,
  MaintainerID,
  SourceID,
  SourceIDs,
  SourcesAdded,
  SourcesRemoved
}
import news.sipyr.eventstore.{
  EventData,
  EventStreams,
  FeedEventExt,
  SourceEventExt
}

import cats.data.Chain
import io.circe.Decoder

object SourceIDsExt {

  def hydrate(events: Chain[EventData]): SourceIDs =
    events.foldLeft(SourceIDs(List.empty)) { (aggregate, eventData) =>
      FeedEventExt
        .decode(eventData)
        .fold(aggregate)(event => fold(aggregate, event))
    }

  def fold(aggregate: SourceIDs, event: FeedEvent): SourceIDs =
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
        SourceIDs(
          aggregate.value.filterNot(source => removedSources.contains(source))
        )
      case FeedEvent.MaintainerChangedCase(_) =>
        aggregate
    }
}
