package news.sipyr.queries

import cats.data.Chain
import cats.effect.IO
import cats.implicits.*
import cats.kernel.Comparison
import news.sipyr.events.SourceIDs
import news.sipyr.eventstore.{EventStream, EventStreams, toEventStreamID}
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait Sources[F[_]] {
  def articles(
      sources: SourceIDs,
      initialized: EpochSeconds,
      from: EpochSeconds,
      to: EpochSeconds
  ): F[Articles]
}

object Sources {
  private class UsingEventStreams(eventStreams: EventStreams[IO])
      extends Sources[IO] {
    val logger = Slf4jLogger.getLogger[IO]

    override def articles(
        sources: SourceIDs,
        initialized: EpochSeconds,
        from: EpochSeconds,
        to: EpochSeconds
    ): IO[Articles] =
      for {
        events <- eventStreams.readMany(
          Chain
            .fromSeq(sources.value)
            .map(sourceID =>
              EventStream(
                sourceID.toEventStreamID,
                EventStream.Categories.sources
              )
            ),
          initialized.toEventsType
        )
        _ <- logger.info(s"Found ${events.length} events for sources $sources.")
        articles = ArticlesExt
          .hydrate(events)
          .toChain
        _ <- logger.info(
          s"Found ${articles.length} articles for sources $sources."
        )
        filtered =
          articles
            .filter(article =>
              (
                article.date.comparison(from),
                article.date.comparison(to)
              ) match {
                case (
                      Comparison.EqualTo | Comparison.GreaterThan,
                      Comparison.EqualTo | Comparison.LessThan
                    ) =>
                  true
                case _ => false
              }
            )
            .toArticles
      } yield filtered
  }

  def usingEventStreams(eventStreams: EventStreams[IO]): Sources[IO] =
    new UsingEventStreams(eventStreams)
}
