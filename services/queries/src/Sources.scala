package news.sipyr.queries

import news.sipyr.events.SourceIDs

trait Sources[F[_]] {
  def articles(
    sources: SourceIDs,
    initialized: EpochSeconds,
    from: EpochSeconds,
    to: EpochSeconds
  ): F[Articles]
}
