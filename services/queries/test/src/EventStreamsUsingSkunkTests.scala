package news.sipyr.queries

import news.sipyr.eventstore.{EventData, EventStream, EventStreams}

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

import java.time.OffsetDateTime
import java.net.ConnectException
import java.util.UUID

class EventStreamsUsingSkunkTests extends CatsEffectSuite {
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

  val underTest: EventStreams[IO] = EventStreams.usingSkunk(session)

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

  test(
    "read returns stream events in ascending time order up to the provided time"
  ) {
    val categoryName = UUID.randomUUID().toString()
    val streamName = UUID.randomUUID().toString()

    val streamState: List[EventStreams.EventRow] = List(
      EventStreams.EventRow(
        1L,
        categoryName,
        streamName,
        0,
        "test_event_type_1",
        "{}"
      ),
      EventStreams.EventRow(
        2L,
        categoryName,
        streamName,
        1,
        "test_event_type_2",
        "{}"
      ),
      EventStreams.EventRow(
        3L,
        categoryName,
        streamName,
        2,
        "test_event_type_3",
        "{}"
      )
    )

    val expected: List[EventData] = List(
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(1L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(streamName),
        EventData.Index.fromInt(0),
        EventData.EventTypeName.fromString("test_event_type_1"),
        EventData.Content.fromJson(Json.obj())
      ),
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(2L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(streamName),
        EventData.Index.fromInt(1),
        EventData.EventTypeName.fromString("test_event_type_2"),
        EventData.Content.fromJson(Json.obj())
      )
    )

    for {
      _ <- ensureDatabaseAvailable
      _ <- insertRows(streamState)
      result <- underTest
        .read(
          EventStream(
            EventStream.ID.fromString(streamName),
            EventStream.Category.fromString(categoryName)
          ),
          EpochSeconds(2L).toEventsType
        )
    } yield assertEquals(
      result.toList,
      expected
    )
  }

  test(
    "readMany returns stream events from all streams in ascending time order up to the provided time"
  ) {
    val categoryName = UUID.randomUUID().toString()
    val streamName1 = UUID.randomUUID().toString()
    val streamName2 = UUID.randomUUID().toString()

    val streamState: List[EventStreams.EventRow] = List(
      EventStreams.EventRow(
        1L,
        categoryName,
        streamName1,
        0,
        "test_event_type_1",
        "{}"
      ),
      EventStreams.EventRow(
        2L,
        categoryName,
        streamName1,
        1,
        "test_event_type_2",
        "{}"
      ),
      EventStreams.EventRow(
        3L,
        categoryName,
        streamName2,
        0,
        "test_event_type_1",
        "{}"
      ),
      EventStreams.EventRow(
        4L,
        categoryName,
        streamName1,
        2,
        "test_event_type_3",
        "{}"
      )
    )

    val expected: List[EventData] = List(
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(1L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(streamName1),
        EventData.Index.fromInt(0),
        EventData.EventTypeName.fromString("test_event_type_1"),
        EventData.Content.fromJson(Json.obj())
      ),
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(2L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(streamName1),
        EventData.Index.fromInt(1),
        EventData.EventTypeName.fromString("test_event_type_2"),
        EventData.Content.fromJson(Json.obj())
      ),
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(3L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(streamName2),
        EventData.Index.fromInt(0),
        EventData.EventTypeName.fromString("test_event_type_1"),
        EventData.Content.fromJson(Json.obj())
      )
    )

    for {
      _ <- ensureDatabaseAvailable
      _ <- insertRows(streamState)
      result <- underTest
        .readMany(
          Chain(
            EventStream(
              EventStream.ID.fromString(streamName1),
              EventStream.Category.fromString(categoryName)
            ),
            EventStream(
              EventStream.ID.fromString(streamName2),
              EventStream.Category.fromString(categoryName)
            )
          ),
          EpochSeconds(3L).toEventsType
        )
    } yield assertEquals(
      result.toList,
      expected
    )
  }

  test(
    "read returns only events for the requested stream when multiple stream names exist in the same category"
  ) {
    val categoryName = UUID.randomUUID().toString()
    val requestedStreamName = UUID.randomUUID().toString()
    val otherStreamName = UUID.randomUUID().toString()

    val streamState: List[EventStreams.EventRow] = List(
      EventStreams.EventRow(
        1L,
        categoryName,
        requestedStreamName,
        0,
        "requested_event_1",
        "{}"
      ),
      EventStreams.EventRow(
        2L,
        categoryName,
        otherStreamName,
        0,
        "other_event_1",
        "{}"
      ),
      EventStreams.EventRow(
        3L,
        categoryName,
        requestedStreamName,
        1,
        "requested_event_2",
        "{}"
      ),
      EventStreams.EventRow(
        4L,
        categoryName,
        otherStreamName,
        1,
        "other_event_2",
        "{}"
      )
    )

    val expected: List[EventData] = List(
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(1L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(requestedStreamName),
        EventData.Index.fromInt(0),
        EventData.EventTypeName.fromString("requested_event_1"),
        EventData.Content.fromJson(Json.obj())
      ),
      EventData(
        EventData.PersistedAt.fromEpochSeconds(EpochSeconds(3L).toEventsType),
        EventData.TypeName.fromString(categoryName),
        EventData.StreamName.fromString(requestedStreamName),
        EventData.Index.fromInt(1),
        EventData.EventTypeName.fromString("requested_event_2"),
        EventData.Content.fromJson(Json.obj())
      )
    )

    for {
      _ <- ensureDatabaseAvailable
      _ <- insertRows(streamState)
      result <- underTest.read(
        EventStream(
          EventStream.ID.fromString(requestedStreamName),
          EventStream.Category.fromString(categoryName)
        ),
        EpochSeconds(4L).toEventsType
      )
    } yield assertEquals(result.toList, expected)
  }
}
