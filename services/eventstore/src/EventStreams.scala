package news.sipyr.eventstore

import news.sipyr.events.EpochSeconds

import cats.data.Chain
import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import io.circe.parser.parse
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk._
import skunk.codec.all._
import skunk.implicits._
import java.time.OffsetDateTime

trait EventStreams[F[_]] {
  def read(eventStream: EventStream, time: EpochSeconds): F[Chain[EventData]]

  def readMany(
      eventStreams: Chain[EventStream],
      time: EpochSeconds
  ): F[Chain[EventData]]

  def streamNames(
      category: EventStream.Category,
      time: EpochSeconds
  ): F[Chain[EventStream.ID]]

  def append(
      eventStream: EventStream,
      persistedAt: EpochSeconds,
      index: Int,
      eventTypeName: EventData.EventTypeName,
      content: EventData.Content
  ): F[Unit]
}

object EventStreams {
  val logger = Slf4jLogger.getLogger[IO]

  final case class EventRow(
      persistedAt: Long,
      typeName: String,
      streamName: String,
      index: Int,
      eventTypeName: String,
      content: String
  )

  val readStream: Query[(Long, String, String), EventRow] =
    sql"""
      SELECT persisted_at, type_name, stream_name, stream_index, event_type_name, content::text
      FROM event_streams
      WHERE
            persisted_at <= $int8
        AND stream_name = ${varchar(128)}
        AND type_name = ${varchar(64)}
      ORDER BY stream_index ASC
    """
      .query(int8 ~ varchar(64) ~ varchar(128) ~ int4 ~ varchar(64) ~ text)
      .map {
        case persistedAt ~ typeName ~ streamName ~ index ~ eventTypeName ~ content =>
          EventRow(
            persistedAt,
            typeName,
            streamName,
            index,
            eventTypeName,
            content
          )
      }

  val readStreamNames: Query[(Long, String), String] =
    sql"""
      SELECT DISTINCT stream_name
      FROM event_streams
      WHERE
            persisted_at <= $int8
        AND type_name = ${varchar(64)}
      ORDER BY stream_name ASC
    """.query(varchar(128))

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

  private class UsingSkunk(session: Resource[IO, Session[IO]])
      extends EventStreams[IO] {
    val logger = Slf4jLogger.getLogger[IO]

    override def read(
        eventStream: EventStream,
        time: EpochSeconds
    ): IO[Chain[EventData]] =
      session
        .use { session =>
          session
            .prepare(readStream)
            .flatMap(
              _.stream(
                (
                  time.secondsSinceEpoch,
                  eventStream.id.streamName,
                  eventStream.category.typeName
                ),
                128
              ).compile.toList
            )
        }
        .flatMap(rows => rows.traverse(decodeRow).map(Chain.fromSeq))
        .onError { error =>
          logger.error(error)(
            s"Failed to read events for stream=${eventStream.id.streamName} category=${eventStream.category.typeName} time=${time.secondsSinceEpoch}"
          )
        }

    override def readMany(
        eventStreams: Chain[EventStream],
        time: EpochSeconds
    ): IO[Chain[EventData]] =
      eventStreams
        .traverse(read(_, time))
        .map(_.foldLeft(Chain.empty[EventData])(_ ++ _))
        .onError { error =>
          logger.error(error)(
            s"Failed to read events for streamCount=${eventStreams.toList.size} time=${time.secondsSinceEpoch}"
          )
        }

    override def streamNames(
        category: EventStream.Category,
        time: EpochSeconds
    ): IO[Chain[EventStream.ID]] =
      session
        .use { session =>
          session
            .prepare(readStreamNames)
            .flatMap(
              _.stream(
                (time.secondsSinceEpoch, category.typeName),
                128
              ).compile.toList
            )
        }
        .map(streamNames =>
          Chain.fromSeq(
            streamNames.map(EventStream.ID.fromString)
          )
        )
        .onError { error =>
          logger.error(error)(
            s"Failed to read stream names for category=${category.typeName} time=${time.secondsSinceEpoch}"
          )
        }

    override def append(
        eventStream: EventStream,
        persistedAt: EpochSeconds,
        index: Int,
        eventTypeName: EventData.EventTypeName,
        content: EventData.Content
    ): IO[Unit] =
      session
        .use { session =>
          session
            .prepare(insertEvent)
            .flatMap(
              _.execute(
                (
                  persistedAt.secondsSinceEpoch,
                  eventStream.category.typeName,
                  eventStream.id.streamName,
                  index,
                  eventTypeName.toEventTypeName,
                  content.toJson.noSpaces
                )
              )
            )
            .void
        }
        .onError { error =>
          logger.error(error)(
            s"Failed to append event type=${eventTypeName.toEventTypeName} stream=${eventStream.id.streamName} category=${eventStream.category.typeName} index=$index"
          )
        }
  }

  private def decodeRow(row: EventRow): IO[EventData] =
    parse(row.content) match {
      case Right(content) =>
        IO.pure(
          EventData(
            EventData.PersistedAt.fromEpochSeconds(
              EpochSeconds(row.persistedAt)
            ),
            EventData.TypeName.fromString(row.typeName),
            EventData.StreamName.fromString(row.streamName),
            EventData.Index.fromInt(row.index),
            EventData.EventTypeName.fromString(row.eventTypeName),
            EventData.Content.fromJson(content)
          )
        )
      case Left(error) =>
        val failure = RuntimeException(
          s"Invalid event JSON for event type ${row.typeName} at index ${row.index}: ${error.getMessage}"
        )
        logger.error(failure)(
          s"Could not decode event payload for type=${row.typeName} index=${row.index}"
        ) *> IO.raiseError(failure)
    }

  def usingSkunk(session: Resource[IO, Session[IO]]): EventStreams[IO] =
    new UsingSkunk(session)
}
