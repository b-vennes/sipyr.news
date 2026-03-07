package news.sipyr.queries

import cats.data.Chain
import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import io.circe.parser.parse
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait EventStreams[F[_]] {
  def read(eventStream: EventStream, time: EpochSeconds): F[Chain[EventData]]

  def readMany(eventStreams: Chain[EventStream], time: EpochSeconds): F[Chain[EventData]]
}

object EventStreams {
  private val logger = Slf4jLogger.getLogger[IO]

  private final case class EventRow(typeName: String, index: Int, content: String)

  private val readByStream: Query[(String, String, Int), EventRow] =
    sql"""
      SELECT type_name, stream_index, content::text
      FROM event_streams
      WHERE stream_name = ${varchar(100)}
        AND type_name = ${varchar(50)}
        AND stream_index <= $int4
      ORDER BY stream_index ASC
    """.query(varchar(50) ~ int4 ~ text).map {
      case typeName ~ index ~ content =>
        EventRow(typeName, index, content)
    }

  private class UsingSkunk(session: Resource[IO, Session[IO]]) extends EventStreams[IO] {
    private val logger = Slf4jLogger.getLogger[IO]

    override def read(eventStream: EventStream, time: EpochSeconds): IO[Chain[EventData]] =
      session.use { session =>
        session.prepare(readByStream).flatMap(
          _.stream(
            (eventStream.id.streamName, eventStream.category.typeName, time.secondsSinceEpoch.toInt),
            128
          ).compile.toList
        )
      }.flatMap(rows =>
        rows.traverse(decodeRow).map(Chain.fromSeq)
      ).onError { error =>
        logger.error(error)(
          s"Failed to read events for stream=${eventStream.id.streamName} category=${eventStream.category.typeName} upToIndex=${time.secondsSinceEpoch}"
        )
      }

    override def readMany(eventStreams: Chain[EventStream], time: EpochSeconds): IO[Chain[EventData]] =
      eventStreams
        .traverse(read(_, time))
        .map(_.foldLeft(Chain.empty[EventData])(_ ++ _))
        .onError { error =>
          logger.error(error)(
            s"Failed to read events for streamCount=${eventStreams.toList.size} upToIndex=${time.secondsSinceEpoch}"
          )
        }
  }

  private def decodeRow(row: EventRow): IO[EventData] =
    parse(row.content) match {
      case Right(content) =>
        IO.pure(
          EventData(
            EventData.TypeName.fromString(row.typeName),
            EventData.Index.fromInt(row.index),
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
