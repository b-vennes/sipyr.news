package news.sipyr.queries

import cats.data.Chain
import cats.effect.std.Env
import cats.effect.{IO, Resource}
import cats.implicits.*
import io.circe.Json
import munit.CatsEffectSuite
import natchez.Trace.Implicits.noop
import news.sipyr.eventstore.{EventData, EventStream, EventStreams}
import skunk.{Command, Session, Void}
import skunk.codec.all.*
import skunk.implicits.*
import news.sipyr.testutils.Database

import java.net.ConnectException
import java.util.UUID

class EventStreamsUsingSkunkTests extends CatsEffectSuite {
  val underTest: EventStreams[IO] = EventStreams.usingSkunk(Database.session)

  test(
    "read returns stream events in ascending time order up to the provided time"
  ) {
    val categoryName = UUID.randomUUID().show
    val streamName = UUID.randomUUID().show

    val requestTime = 2L

    val streamState = Chain(
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

    val expected: Chain[EventData] = Chain(
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
      _ <- Database.ensureAvailable
      _ <- Database.insertEventRows(streamState)
      result <- underTest
        .read(
          EventStream(
            EventStream.ID.fromString(streamName),
            EventStream.Category.fromString(categoryName)
          ),
          EpochSeconds(requestTime).toEventsType
        )
    } yield assertEquals(result, expected)
  }

  test(
    "readMany returns stream events from all streams in ascending time order up to the provided time"
  ) {
    val categoryName = UUID.randomUUID().show
    val streamName1 = UUID.randomUUID().show
    val streamName2 = UUID.randomUUID().show

    val streamState: Chain[EventStreams.EventRow] = Chain(
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

    val expected: Chain[EventData] = Chain(
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
      _ <- Database.ensureAvailable
      _ <- Database.insertEventRows(streamState)
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
    } yield assertEquals(result, expected)
  }

  test(
    "read returns only events for the requested stream when multiple stream names exist in the same category"
  ) {
    val categoryName = UUID.randomUUID().show
    val requestedStreamName = UUID.randomUUID().show
    val otherStreamName = UUID.randomUUID().show

    val streamState: Chain[EventStreams.EventRow] = Chain(
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

    val expected: Chain[EventData] = Chain(
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
      _ <- Database.ensureAvailable
      _ <- Database.insertEventRows(streamState)
      result <- underTest.read(
        EventStream(
          EventStream.ID.fromString(requestedStreamName),
          EventStream.Category.fromString(categoryName)
        ),
        EpochSeconds(4L).toEventsType
      )
    } yield assertEquals(result, expected)
  }
}
