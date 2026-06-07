package news.sipyr.eventstore

import news.sipyr.events.SourceID

extension (sourceID: SourceID) {
  def toEventStreamID: EventStream.ID =
    EventStream.ID.fromString(sourceID.value.toString())
}
