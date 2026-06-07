package news.sipyr.queries

import cats.data.Chain
import news.sipyr.events.{
  SourceID,
  SourceIDs,
  EpochSeconds as EventsEpochSeconds
}

import java.time.{Instant, OffsetDateTime}
import scala.annotation.targetName

val secondsInDay = 60 * 60 * 24

extension (epochSeconds: EpochSeconds) {
  def dayBefore: EpochSeconds =
    EpochSeconds(
      epochSeconds.secondsSinceEpoch - secondsInDay
    )

  @targetName("epochSecondsToEventsType")
  def toEventsType: EventsEpochSeconds = EventsEpochSeconds(
    epochSeconds.secondsSinceEpoch
  )
}

extension (epochSeconds: EpochSeconds.type) {
  def fromStringUnsafe(value: String): EpochSeconds =
    EpochSeconds(
      OffsetDateTime.parse(value).toEpochSecond()
    )

  def fromEventsType(value: EventsEpochSeconds): EpochSeconds =
    EpochSeconds(value.secondsSinceEpoch)
}

extension (articles: Articles) {
  @targetName("articlesToChain")
  def toChain: Chain[Article] = Chain.fromSeq(articles.value)
}

extension (articlesChain: Chain[Article]) {
  @targetName("articlesChainToArticles")
  def toArticles: Articles = Articles(articlesChain.toList)
}
