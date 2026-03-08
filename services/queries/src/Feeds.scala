package news.sipyr.queries

import news.sipyr.events.SourceIDs

import cats.effect.IO

trait Feeds[F[_]] {
  def sources(feedName: String, time: EpochSeconds): F[SourceIDs]
}

object Feeds {
  private class UsingEventStreams(eventStreams: EventStreams[IO])
      extends Feeds[IO] {
    override def sources(feedName: String, time: EpochSeconds): IO[SourceIDs] =
      for {
        events <- eventStreams.read(
          EventStream(
            EventStream.ID.fromString(feedName),
            EventStream.Categories.feeds
          ),
          time
        )
        sourceIDs = SourceIDsExt.hydrate(events)
      } yield sourceIDs
  }

  def usingEventStreams(eventStreams: EventStreams[IO]): Feeds[IO] =
    new UsingEventStreams(eventStreams)
}
