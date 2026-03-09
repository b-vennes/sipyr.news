package news.sipyr.queries

import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  RSSLocation,
  SourceEvent,
  SourceID,
  SourceInitialized,
  SourceLocation
}

import cats.data.Chain
import cats.implicits._
import io.circe.Decoder

object ArticlesExt {
  private given Decoder[news.sipyr.events.EpochSeconds] =
    Decoder.forProduct1("secondsSinceEpoch")(news.sipyr.events.EpochSeconds(_))
  private given Decoder[RSSLocation] =
    Decoder.forProduct1("url")(RSSLocation(_))
  private given Decoder[SourceLocation] = Decoder.instance(cursor =>
    cursor.downField("rss").as[RSSLocation].map(SourceLocation.rss)
  )
  private given Decoder[SourceID] = Decoder[Long].map(SourceID(_))
  private given Decoder[SourceInitialized] =
    Decoder.forProduct2("id", "location")(SourceInitialized(_, _))
  private given Decoder[ArticleDefinition] =
    Decoder.forProduct6("id", "name", "author", "outlet", "url", "date")(
      ArticleDefinition(_, _, _, _, _, _)
    )
  private given Decoder[ArticlesAdded] =
    Decoder.forProduct1("articles")(ArticlesAdded(_))

  val initializedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("initialized")
  val articlesAddedEventType: EventData.EventTypeName =
    EventData.EventTypeName.fromString("articlesAdded")

  def fromChain(articlesChain: Chain[Article]): Articles = Articles(
    articlesChain.toList
  )

  def hydrate(events: Chain[EventData]): Articles =
    fromChain(events.foldLeft(Chain.empty[Article]) { (aggregate, eventData) =>
      decode(eventData).fold(aggregate) { event =>
        fold(aggregate, event)
      }
    })

  def decode(eventData: EventData): Option[SourceEvent] =
    eventData.eventTypeName match {
      case t if t === initializedEventType =>
        eventData.content.toJson
          .as[SourceInitialized]
          .toOption
          .map(SourceEvent.initialized)
      case t if t === articlesAddedEventType =>
        eventData.content.toJson
          .as[ArticlesAdded]
          .toOption
          .map(SourceEvent.articlesAdded)
      case _ => None
    }

  def fold(
      aggregate: Chain[Article],
      event: SourceEvent,
  ): Chain[Article] =
    event match {
      case SourceEvent.InitializedCase(_) =>
        aggregate
      case SourceEvent.ArticlesAddedCase(articlesAdded) =>
        aggregate ++ Chain.fromSeq(
          articlesAdded.articles.map(toArticle)
        )
    }

  def toArticle(definition: ArticleDefinition): Article =
    Article(
      id = definition.id,
      name = definition.name,
      author = definition.author,
      outlet = definition.outlet,
      url = definition.url,
      date = EpochSeconds(definition.date.secondsSinceEpoch)
    )
}
