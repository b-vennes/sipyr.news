package news.sipyr.queries

import news.sipyr.events.SourceIDs

import cats.data.Chain
import cats.effect.IO
import cats.kernel.Comparison
import cats.implicits._

trait Sources[F[_]] {
  def articles(
    sources: SourceIDs,
    initialized: EpochSeconds,
    from: EpochSeconds,
    to: EpochSeconds
  ): F[Articles]
}

object Sources {
  private class UsingEventStreams(eventStreams: EventStreams[IO]) extends Sources[IO] {
    override def articles(
      sources: SourceIDs,
      initialized: EpochSeconds,
      from: EpochSeconds,
      to: EpochSeconds): IO[Articles] =
      for {
        events <- eventStreams.readMany(
          Chain.fromSeq(sources.value).map(sourceID =>
            EventStream(
              sourceID.toEventStreamID,
              EventStream.Categories.sources
            )
          ),
          initialized)
        articles =
          Articles
            .hydrate(events)
            .toChain
            .filter(article =>
              (article.date.comparison(from), article.date.comparison(to)) match {
                case (Comparison.EqualTo | Comparison.GreaterThan, Comparison.EqualTo | Comparison.LessThan) =>
                  true
                case _ => false
              }
            )
            .toArticles
      } yield articles
  }

  def usingEventStreams(eventStreams: EventStreams[IO]): Sources[IO] = new UsingEventStreams(eventStreams)
}
