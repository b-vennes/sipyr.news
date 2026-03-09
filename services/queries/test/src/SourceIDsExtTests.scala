package news.sipyr.queries

import cats.data.Chain
import io.circe.parser.parse
import munit.FunSuite
import news.sipyr.events.{SourceID, SourceIDs}

class SourceIDsExtTests extends FunSuite {
  test("hydrate decodes feed events and folds source ids in order") {
    val events = Chain(
      event(
        streamName = "daily-feed",
        eventTypeName = "created",
        content = """{
            |  "maintainer": 99,
            |  "name": "Daily Feed",
            |  "sources": ["source-a", "source-b"]
            |}""".stripMargin
      ),
      event(
        streamName = "daily-feed",
        eventTypeName = "sourcesAdded",
        content = """{ "sources": ["source-c"] }"""
      ),
      event(
        streamName = "daily-feed",
        eventTypeName = "sourcesRemoved",
        content = """{ "sources": ["source-a"] }"""
      ),
      event(
        streamName = "daily-feed",
        eventTypeName = "maintainerChanged",
        content = """{ "maintainer": 101 }"""
      ),
      event(
        streamName = "daily-feed",
        eventTypeName = "sourcesAdded",
        content = """{ "sources": "bad-shape" }"""
      ),
      event(
        streamName = "daily-feed",
        eventTypeName = "unknownEvent",
        content = """{ "sources": ["source-z"] }"""
      )
    )

    val hydrated = SourceIDsExt.hydrate(events)

    assertEquals(
      hydrated,
      SourceIDs(List(SourceID("source-b"), SourceID("source-c")))
    )
  }

  def event(
      streamName: String,
      eventTypeName: String,
      content: String
  ): EventData =
    EventData(
      persistedAt = EventData.PersistedAt.fromEpochSeconds(EpochSeconds(1L)),
      typeName = EventData.TypeName.fromString("feeds"),
      streamName = EventData.StreamName.fromString(streamName),
      index = EventData.Index.fromInt(0),
      eventTypeName = EventData.EventTypeName.fromString(eventTypeName),
      content = EventData.Content.fromJson(
        parse(content).fold(throw _, identity)
      )
    )
}
