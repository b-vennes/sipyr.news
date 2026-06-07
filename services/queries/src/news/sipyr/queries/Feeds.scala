package news.sipyr.queries

import cats.effect.IO
import news.sipyr.events.{SourceIDs, EpochSeconds as EventsEpochSeconds}
import news.sipyr.eventstore.{EventStream, EventStreams}
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait Feeds[F[_]] {
  def sources(feedName: String, time: EpochSeconds): F[SourceIDs]
}

object Feeds {
  private class UsingEventStreams(eventStreams: EventStreams[IO])
      extends Feeds[IO] {
    val logger = Slf4jLogger.getLogger[IO]

    override def sources(feedName: String, time: EpochSeconds): IO[SourceIDs] =
      for {
        events <- eventStreams.read(
          EventStream(
            EventStream.ID.fromString(feedName),
            EventStream.Categories.feeds
          ),
          EventsEpochSeconds(time.secondsSinceEpoch)
        )
        _ <- logger.info(s"Found ${events.length} events for feed $feedName.")
        sourceIDs = SourceIDsExt.hydrate(events)
      } yield sourceIDs
  }

  def usingEventStreams(eventStreams: EventStreams[IO]): Feeds[IO] =
    new UsingEventStreams(eventStreams)
}
