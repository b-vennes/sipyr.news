package news.sipyr.sourcing

import cats.data.Chain
import cats.effect.{IO, Resource}
import io.circe.Json
import munit.CatsEffectSuite
import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  EpochSeconds,
  SourceID
}
import news.sipyr.eventstore.{EventStreams, toEventStreamID}
import news.sipyr.testutils.Database

import java.net.ConnectException
import java.util.UUID

class SourcingServiceUsingSkunkTests extends CatsEffectSuite {

  test("pollOnce appends articlesAdded to the source stream in postgres") {
    val sourceID = SourceID(s"source-${UUID.randomUUID().toString}")
    val article = ArticleDefinition(
      id = 101L,
      name = "New article",
      author = "Author",
      outlet = "Outlet",
      url = s"https://example.com/articles/${UUID.randomUUID().toString}",
      date = EpochSeconds(12345L)
    )

    val eventStreams = EventStreams.usingSkunk(Database.session)
    val sources = Sources.usingEventStreams(eventStreams)
    val underTest = SourcingService.create(
      sources = sources,
      feedClient = new TestFeedClient(Chain.one(article))
    )

    for {
      _ <- Database.ensureAvailable
      _ <- Database.insertEventRows(
        Chain(
          sourceInitialized(persistedAt = 100L, sourceID = sourceID, index = 0)
        )
      )
      singleSourceState <- sources
        .withID(sourceID)
        .map(source => SourcingState(Map(sourceID -> source)))
      firstPass <- underTest.pollOnce.run(singleSourceState)
      hydratedSource <- sources.withID(sourceID)
      secondPass <- underTest.pollOnce.run(firstPass._1)
    } yield {
      assertEquals(
        firstPass._2,
        Chain(sourceID -> ArticlesAdded(List(article)))
      )
      assertEquals(
        hydratedSource.source.articles,
        Chain(Article.atLocation(article.url))
      )
      assertEquals(hydratedSource.nextIndex, 2)
      assertEquals(secondPass._2, Chain.empty)
    }
  }

  def sourceInitialized(
      persistedAt: Long,
      sourceID: SourceID,
      index: Int
  ): EventStreams.EventRow =
    EventStreams.EventRow(
      persistedAt = persistedAt,
      typeName = "sources",
      streamName = sourceID.value,
      index = index,
      eventTypeName = "initialized",
      content = Json
        .obj(
          "location" -> Json.obj(
            "rss" -> Json.obj(
              "url" -> Json.fromString(s"https://example.com/${sourceID.value}")
            )
          )
        )
        .noSpaces
    )

  private class TestFeedClient(result: Chain[ArticleDefinition])
      extends FeedClient[IO] {
    override def articles(source: Source): IO[Chain[ArticleDefinition]] =
      IO.pure(result)
  }
}
