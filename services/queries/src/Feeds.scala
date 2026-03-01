package news.sipyr.queries

import news.sipyr.events.SourceIDs

trait Feeds[F[_]] {
  def sources(feedName: String): F[SourceIDs]
}
