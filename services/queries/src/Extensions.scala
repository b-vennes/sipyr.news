package news.sipyr.queries

import news.sipyr.events.{SourceID, SourceIDs}

import cats.data.Chain

import scala.annotation.targetName
import java.time.Instant
import java.time.OffsetDateTime

val secondsInDay = 60 * 60 * 24

extension (epochSeconds: EpochSeconds) {
    def dayBefore: EpochSeconds =
      EpochSeconds(
        epochSeconds.secondsSinceEpoch - secondsInDay
      )
}

extension (epochSeconds: EpochSeconds.type) {
  def fromStringUnsafe(value: String): EpochSeconds =
    EpochSeconds(
      OffsetDateTime.parse(value).toEpochSecond()
    )
}

extension (sourceIDs: SourceIDs.type) {
  def hydrate(events: Chain[EventData]): SourceIDs = SourceIDs(List.empty)
}

extension (sourceID: SourceID) {
  def toEventStreamID: EventStream.ID = EventStream.ID.fromString(sourceID.value.toString())
}

extension (articles: Articles) {
  @targetName("articlesToChain")
  def toChain: Chain[Article] = Chain.fromSeq(articles.value)
}

extension (articles: Articles.type) {
  def fromChain(articlesChain: Chain[Article]): Articles = Articles(articlesChain.toList)

  def hydrate(events: Chain[EventData]): Articles = Articles(List.empty)
}

extension (articlesChain: Chain[Article]) {
  @targetName("articlesChainToArticles")
  def toArticles: Articles = Articles(articlesChain.toList)
}
