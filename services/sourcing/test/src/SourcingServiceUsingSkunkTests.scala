package news.sipyr.sourcing

import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  EpochSeconds,
  SourceID
}
import news.sipyr.eventstore.{EventStream, EventStreams, toEventStreamID}

import cats.data.Chain
import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import io.circe.Json
import munit.CatsEffectSuite
import natchez.Trace.Implicits.noop
import skunk.Command
import skunk.Session
import skunk.Void
import skunk.codec.all._
import skunk.implicits._

import java.net.ConnectException
import java.util.UUID

class SourcingServiceUsingSkunkTests extends CatsEffectSuite {
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

  def ensureDatabaseAvailable: IO[Unit] =
    session
      .use(
        _.prepare(sql"select 1".query(int4)).flatMap(_.unique(Void)).void
      )
      .handleErrorWith {
        case error if isConnectionRefused(error) =>
          IO.println(
            "Connection refused while running database integration tests. Start the root docker compose environment before running tests."
          ) *> IO.raiseError(error)
        case error =>
          IO.raiseError(error)
      }

  def isConnectionRefused(error: Throwable): Boolean =
    Iterator
      .iterate(Option(error))(_.flatMap(err => Option(err.getCause)))
      .takeWhile(_.nonEmpty)
      .flatten
      .exists(err =>
        err.isInstanceOf[ConnectException] ||
          Option(err.getMessage).exists(_.contains("Connection refused"))
      )

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

    val eventStreams = EventStreams.usingSkunk(session)
    val sources = Sources.usingEventStreams(eventStreams)
    val underTest = SourcingService.create(
      sources = sources,
      feedClient = new TestFeedClient(Chain.one(article))
    )

    for {
      _ <- ensureDatabaseAvailable
      _ <- insertRows(
        List(
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
        hydratedSource.source.articles.toList,
        List(Article.atLocation(article.url))
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
