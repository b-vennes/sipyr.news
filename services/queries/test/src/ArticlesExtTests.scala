package news.sipyr.queries

import news.sipyr.eventstore.EventData

import cats.data.Chain
import io.circe.parser.parse
import munit.FunSuite

class ArticlesExtTests extends FunSuite {
  test("hydrate decodes source events and folds articles in order") {
    val events = Chain(
      event(
        streamName = "wired.com/rss",
        eventTypeName = "initialized",
        content = """{
            |  "id": "wired.com/rss",
            |  "location": {
            |    "rss": {
            |      "url": "https://wired.com/rss"
            |    }
            |  }
            |}""".stripMargin
      ),
      event(
        streamName = "wired.com/rss",
        eventTypeName = "articlesAdded",
        content = """{
            |"articles": [
            |  {
            |    "id": 10,
            |    "name": "First",
            |    "author": "Alice",
            |    "outlet": "Wired",
            |    "url":"https://wired.com/a",
            |    "date": {
            |      "secondsSinceEpoch": 100
            |    }
            |  },
            |  {
            |    "id": 11,
            |    "name": "Second",
            |    "author": "Bob",
            |    "outlet": "Wired",
            |    "url": "https://wired.com/b",
            |    "date": {
            |      "secondsSinceEpoch": 101
            |    }
            |  }
            |]}""".stripMargin
      ),
      event(
        streamName = "wired.com/rss",
        eventTypeName = "articlesAdded",
        content = """{
            |  "articles": "bad-shape"
            |}""".stripMargin
      ),
      event(
        streamName = "nyt.com/rss",
        eventTypeName = "articlesAdded",
        content = """{
            |  "articles": [
            |    {
            |      "id": 20,
            |      "name": "Third",
            |      "author": "Carol",
            |      "outlet": "New York Times",
            |      "url": "https://nytimes.com/c",
            |      "date": {
            |        "secondsSinceEpoch": 102
            |      }
            |    }
            |  ]
            |}""".stripMargin
      ),
      event(
        streamName = "wired.com/rss",
        eventTypeName = "otherType",
        content = "{}"
      )
    )

    val hydrated = ArticlesExt.hydrate(events)

    assertEquals(
      hydrated.value,
      List(
        Article(
          id = 10L,
          name = "First",
          author = "Alice",
          outlet = "Wired",
          url = "https://wired.com/a",
          date = EpochSeconds(100L)
        ),
        Article(
          id = 11L,
          name = "Second",
          author = "Bob",
          outlet = "Wired",
          url = "https://wired.com/b",
          date = EpochSeconds(101L)
        ),
        Article(
          id = 20L,
          name = "Third",
          author = "Carol",
          outlet = "New York Times",
          url = "https://nytimes.com/c",
          date = EpochSeconds(102L)
        )
      )
    )
  }

  def event(
      streamName: String,
      eventTypeName: String,
      content: String
  ): EventData =
    EventData(
      persistedAt =
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(1L).toEventsType),
      typeName = EventData.TypeName.fromString("sources"),
      streamName = EventData.StreamName.fromString(streamName),
      index = EventData.Index.fromInt(0),
      eventTypeName = EventData.EventTypeName.fromString(eventTypeName),
      content = EventData.Content.fromJson(
        parse(content).fold(throw _, identity)
      )
    )
}
