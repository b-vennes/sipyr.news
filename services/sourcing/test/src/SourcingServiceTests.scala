package news.sipyr.sourcing

import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  EpochSeconds,
  SourceID
}

import cats.data.Chain
import cats.data.NonEmptyChain
import cats.effect.IO
import cats.effect.Ref
import cats.implicits._
import munit.CatsEffectSuite

class SourcingServiceTests extends CatsEffectSuite {
  test("pollOnce emits new articles once and keeps dedupe state in memory") {
    val sourceID = SourceID("source-1")
    val source = StoredSource(
      id = sourceID,
      source = Source(
        Source.Location.RSS("https://example.com/feed.xml"),
        Chain(Article.atLocation("https://example.com/already-known"))
      ),
      nextIndex = 3
    )
    val fetchedArticles = Chain(
      article(1L, "https://example.com/already-known"),
      article(2L, "https://example.com/new-article")
    )

    for {
      appended <- Ref.of[IO, List[(SourceID, Int, Chain[ArticleDefinition])]](
        Nil
      )
      underTest = SourcingService.create(
        sources = new TestSources(source, appended),
        feedClient = new TestFeedClient(fetchedArticles)
      )
      initialState = SourcingState(Map(sourceID -> source))
      firstPass <- underTest.pollOnce.run(initialState)
      secondPass <- underTest.pollOnce.run(firstPass._1)
      appendedEvents <- appended.get
    } yield {
      assertEquals(
        firstPass._2.toList,
        List(
          sourceID -> ArticlesAdded(
            List(article(2L, "https://example.com/new-article"))
          )
        )
      )
      assertEquals(secondPass._2.toList, Nil)
      assertEquals(appendedEvents.length, 1)
      assertEquals(appendedEvents.head._1, sourceID)
      assertEquals(appendedEvents.head._2, 3)
      assertEquals(
        appendedEvents.head._3.toList.map(_.url),
        List("https://example.com/new-article")
      )
    }
  }

  private def article(id: Long, url: String): ArticleDefinition =
    ArticleDefinition(
      id = id,
      name = s"article-$id",
      author = "Author",
      outlet = "Outlet",
      url = url,
      date = EpochSeconds(100L + id)
    )

  private class TestFeedClient(result: Chain[ArticleDefinition])
      extends FeedClient[IO] {
    override def articles(source: Source): IO[Chain[ArticleDefinition]] =
      IO.pure(result)
  }

  private class TestSources(
      source: StoredSource,
      appended: Ref[IO, List[(SourceID, Int, Chain[ArticleDefinition])]]
  ) extends Sources[IO] {
    override def all: IO[Chain[StoredSource]] =
      IO.pure(Chain.one(source))

    override def withID(id: SourceID): IO[StoredSource] =
      IO.pure(source)

    override def addArticles(
        id: SourceID,
        expectedIndex: Int,
        persistedAt: EpochSeconds,
        articles: NonEmptyChain[ArticleDefinition]
    ): IO[Unit] =
      appended.update(_ :+ (id, expectedIndex, Chain.fromSeq(articles.toList)))
  }
}
