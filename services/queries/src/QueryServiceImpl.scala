package news.sipyr.queries

import cats.effect.IO
import cats.implicits._
import cats.data.Chain
import org.typelevel.log4cats.slf4j.Slf4jLogger

class QueryServiceImpl(
    feeds: Feeds[IO],
    sources: Sources[IO]
) extends QueryService[IO] {
  val logger = Slf4jLogger.getLogger[IO]

  override def frontPage(
      feedName: String,
      page: Int,
      pageSize: Int,
      initialized: EpochSeconds
  ): IO[FrontPageResponse] =
    for {
      feedSources <- feeds.sources(feedName, initialized)
      _ <- logger.info(s"Found sources: ${feedSources}")
      articles <- sources.articles(
        feedSources,
        initialized,
        initialized.dayBefore,
        initialized
      )
      _ <- logger.info(s"Found articles: ${articles}")
      pageRange = Range((page - 1) * pageSize, page * pageSize)
      paged = Chain
        .fromSeq(articles.value)
        .zipWithIndex
        .filter((article, index) => pageRange.contains(index))
        .map(_._1)
    } yield FrontPageResponse(paged.toList)
}
