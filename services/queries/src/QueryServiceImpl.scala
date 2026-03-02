package news.sipyr.queries

import cats.effect.IO
import cats.implicits._
import cats.data.Chain

class QueryServiceImpl(
  feeds: Feeds[IO],
  sources: Sources[IO]
  ) extends QueryService[IO] {
  override def frontPage(
    feedName: String,
    page: Int,
    pageSize: Int,
    initialized: EpochSeconds): IO[FrontPageResponse] =
    for {
      feedSources <- feeds.sources(feedName, initialized)
      articles <- sources.articles(
        feedSources,
        initialized,
        initialized.dayBefore,
        initialized)
      pageRange = Range((page - 1) * pageSize, page * pageSize)
      paged = Chain.fromSeq(articles.value)
        .zipWithIndex
        .filter((article, index) => pageRange.contains(index))
        .map(_._1)
    } yield FrontPageResponse(paged.toList)
}
