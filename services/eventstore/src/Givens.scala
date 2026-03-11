package news.sipyr.eventstore

import news.sipyr.events.*

import cats.implicits._
import io.circe.Decoder

given Decoder[MaintainerID] = Decoder[Long].map(MaintainerID(_))
given Decoder[SourceID] = Decoder[String].map(SourceID(_))
given Decoder[FeedCreated] =
  Decoder.forProduct3("maintainer", "name", "sources")(FeedCreated(_, _, _))
given Decoder[SourcesAdded] =
  Decoder.forProduct1("sources")(SourcesAdded(_))
given Decoder[SourcesRemoved] =
  Decoder.forProduct1("sources")(SourcesRemoved(_))
given Decoder[MaintainerChanged] =
  Decoder.forProduct1("maintainer")(MaintainerChanged(_))

given Decoder[EpochSeconds] =
  Decoder.forProduct1("secondsSinceEpoch")(news.sipyr.events.EpochSeconds(_))
given Decoder[RSSLocation] =
  Decoder.forProduct1("url")(RSSLocation(_))
given Decoder[SourceLocation] = Decoder.instance(cursor =>
  cursor.downField("rss").as[RSSLocation].map(SourceLocation.rss)
)
given Decoder[SourceInitialized] =
  Decoder.forProduct1("location")(SourceInitialized(_))
given Decoder[ArticleDefinition] =
  Decoder.forProduct6("id", "name", "author", "outlet", "url", "date")(
    ArticleDefinition(_, _, _, _, _, _)
  )
given Decoder[ArticlesAdded] =
  Decoder.forProduct1("articles")(ArticlesAdded(_))
