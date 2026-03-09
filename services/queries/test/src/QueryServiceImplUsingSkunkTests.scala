package news.sipyr.queries

import news.sipyr.events.SourceID

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import munit.CatsEffectSuite
import natchez.Trace.Implicits.noop
import skunk.Command
import skunk.Session
import skunk.Void
import skunk.codec.all._
import skunk.implicits._
import io.circe.Json

import java.util.UUID

class QueryServiceImplUsingSkunkTests extends CatsEffectSuite {
  val databaseHost: String =
    sys.env.getOrElse("TEST_POSTGRES_HOST", "localhost")
  val databasePort: Int =
    sys.env.get("TEST_POSTGRES_PORT").flatMap(_.toIntOption).getOrElse(5432)
  val databaseUser: String =
    sys.env.getOrElse("TEST_POSTGRES_USER", "postgres")
  val databaseName: String =
    sys.env.getOrElse("TEST_POSTGRES_DATABASE", "postgres")
  val databasePassword: String =
    sys.env.getOrElse("TEST_POSTGRES_PASSWORD", "pass")

  val session: Resource[IO, Session[IO]] =
    Session.single[IO](
      host = databaseHost,
      port = databasePort,
      user = databaseUser,
      database = databaseName,
      password = Some(databasePassword)
    )

  val insertEvent: Command[(Long, String, String, Int, String, String)] =
    sql"""
      INSERT INTO event_streams
      (
        persisted_at,
        type_name,
        stream_name,
        stream_index,
        event_type_name,
        content
      )
      VALUES (
        $int8,
        ${varchar(64)},
        ${varchar(128)},
        $int4,
        ${varchar(64)},
        ${text}::json
      )
    """.command

  val queryService: QueryServiceImpl = {
    val eventStreams = EventStreams.usingSkunk(session)
    val feeds = Feeds.usingEventStreams(eventStreams)
    val sources = Sources.usingEventStreams(eventStreams)
    QueryServiceImpl(feeds, sources)
  }

  def insertRows(rows: List[EventStreams.EventRow]): IO[Unit] =
    session.use { s =>
      s.prepare(insertEvent)
        .flatMap(command =>
          rows.traverse_(row =>
            command
              .execute(
                (
                  row.persistedAt,
                  row.typeName,
                  row.streamName,
                  row.index,
                  row.eventTypeName,
                  row.content
                )
              )
              .void
          )
        )
    }

  test(
    "frontPage returns paged articles from feed sources filtered by date range"
  ) {
    val initialized = EpochSeconds(200000L)
    val feedName = s"feed-${UUID.randomUUID().toString}"
    val sourceID1 = randomSourceID()
    val sourceID2 = randomSourceID()

    val rows = List(
      feedCreated(
        persistedAt = 100L,
        feedName = feedName,
        index = 0,
        sourceIDs = List(sourceID1, sourceID2)
      ),
      sourceInitialized(persistedAt = 101L, sourceID = sourceID1, index = 0),
      sourceArticlesAdded(
        persistedAt = 102L,
        sourceID = sourceID1,
        index = 1,
        articles = List(
          ArticleSeed(1L, "A1", 190000L),
          ArticleSeed(2L, "A2", 210000L),
          ArticleSeed(3L, "A3", 120000L)
        )
      ),
      sourceInitialized(persistedAt = 103L, sourceID = sourceID2, index = 0),
      sourceArticlesAdded(
        persistedAt = 104L,
        sourceID = sourceID2,
        index = 1,
        articles = List(
          ArticleSeed(4L, "B1", 113600L),
          ArticleSeed(5L, "B2", 113599L),
          ArticleSeed(6L, "B3", 200000L)
        )
      )
    )

    for {
      _ <- insertRows(rows)
      firstPage <- queryService.frontPage(
        feedName = feedName,
        page = 1,
        pageSize = 2,
        initialized = initialized
      )
      secondPage <- queryService.frontPage(
        feedName = feedName,
        page = 2,
        pageSize = 2,
        initialized = initialized
      )
    } yield {
      assertEquals(firstPage.articles.map(_.id), List(1L, 3L))
      assertEquals(secondPage.articles.map(_.id), List(4L, 6L))
    }
  }

  test(
    "frontPage uses initialized time as read snapshot for feeds and sources"
  ) {
    val initialized = EpochSeconds(100000L)
    val feedName = s"feed-${UUID.randomUUID().toString}"
    val sourceID1 = randomSourceID()
    val sourceID2 = randomSourceID()

    val rows = List(
      feedCreated(
        persistedAt = 10L,
        feedName = feedName,
        index = 0,
        sourceIDs = List(sourceID1)
      ),
      feedSourcesAdded(
        persistedAt = 100001L,
        feedName = feedName,
        index = 1,
        sourceIDs = List(sourceID2)
      ),
      sourceInitialized(persistedAt = 11L, sourceID = sourceID1, index = 0),
      sourceArticlesAdded(
        persistedAt = 12L,
        sourceID = sourceID1,
        index = 1,
        articles = List(ArticleSeed(10L, "before-snapshot", 99999L))
      ),
      sourceArticlesAdded(
        persistedAt = 100001L,
        sourceID = sourceID1,
        index = 2,
        articles = List(ArticleSeed(11L, "after-snapshot", 99998L))
      ),
      sourceInitialized(persistedAt = 20L, sourceID = sourceID2, index = 0),
      sourceArticlesAdded(
        persistedAt = 21L,
        sourceID = sourceID2,
        index = 1,
        articles = List(ArticleSeed(12L, "source-added-later", 99997L))
      )
    )

    for {
      _ <- insertRows(rows)
      result <- queryService.frontPage(
        feedName = feedName,
        page = 1,
        pageSize = 10,
        initialized = initialized
      )
    } yield assertEquals(result.articles.map(_.id), List(10L))
  }

  def randomSourceID(): SourceID =
    SourceID(UUID.randomUUID().toString())

  final case class ArticleSeed(id: Long, name: String, date: Long)

  def feedCreated(
      persistedAt: Long,
      feedName: String,
      index: Int,
      sourceIDs: List[SourceID]
  ): EventStreams.EventRow =
    EventStreams.EventRow(
      persistedAt = persistedAt,
      typeName = "feeds",
      streamName = feedName,
      index = index,
      eventTypeName = "created",
      content = Json
        .obj(
          "maintainer" -> Json.fromInt(1),
          "name" -> Json.fromString(feedName),
          "sources" -> Json.arr(sourceIDs.map(_.value).map(Json.fromString)*)
        )
        .noSpaces
    )

  def feedSourcesAdded(
      persistedAt: Long,
      feedName: String,
      index: Int,
      sourceIDs: List[SourceID]
  ): EventStreams.EventRow =
    EventStreams.EventRow(
      persistedAt = persistedAt,
      typeName = "feeds",
      streamName = feedName,
      index = index,
      eventTypeName = "sourcesAdded",
      content = Json
        .obj(
          "sources" -> Json.arr(sourceIDs.map(_.value).map(Json.fromString)*)
        )
        .noSpaces
    )

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

  def sourceArticlesAdded(
      persistedAt: Long,
      sourceID: SourceID,
      index: Int,
      articles: List[ArticleSeed]
  ): EventStreams.EventRow =
    EventStreams.EventRow(
      persistedAt = persistedAt,
      typeName = "sources",
      streamName = sourceID.value,
      index = index,
      eventTypeName = "articlesAdded",
      content = Json
        .obj(
          "articles" -> Json.arr(articles.map(articleJson)*)
        )
        .noSpaces
    )

  def articleJson(article: ArticleSeed): Json =
    Json
      .obj(
        "id" -> Json.fromLong(article.id),
        "name" -> Json.fromString(article.name),
        "author" -> Json.fromString("Author"),
        "outlet" -> Json.fromString("Outlet"),
        "url" -> Json.fromString(s"https://example.com/articles/${article.id}"),
        "date" -> Json.obj(
          "secondsSinceEpoch" -> Json.fromLong(article.date)
        )
      )
}
