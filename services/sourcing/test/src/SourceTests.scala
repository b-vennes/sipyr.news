package news.sipyr.sourcing

import munit.FunSuite
import news.sipyr.events.SourceEvent
import news.sipyr.events.ArticlesAdded
import news.sipyr.events.ArticleDefinition
import news.sipyr.events.EpochSeconds
import cats.data.Chain
import news.sipyr.events.SourceInitialized
import news.sipyr.events.SourceLocation
import news.sipyr.events.RSSLocation
import cats.implicits._

class SourceTests extends FunSuite {
  test("fold with none state and initialized returns some source") {
    val initial = none[Source]

    val event = SourceEvent.initialized(
      SourceInitialized(
        SourceLocation.rss(RSSLocation("test.location"))
      )
    )

    val expected: Option[Source] = Some(
      Source(
        Source.Location.RSS("test.location"),
        Chain.empty
      )
    )

    val result = Source.fold(initial, event)

    assertEquals(result, expected)
  }

  test("fold with none state and articles added returns none source") {
    val initial = none[Source]

    val event = SourceEvent.articlesAdded(
      ArticlesAdded(
        List(
          ArticleDefinition(
            1L,
            "Test Article",
            "Test Author",
            "Test Outlet",
            "test.location",
            EpochSeconds(100L)
          )
        )
      )
    )

    val expected: Option[Source] = None

    val result = Source.fold(initial, event)

    assertEquals(result, expected)
  }
}
