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
import news.sipyr.eventstore.{EventData, SourceEventExt}

import cats.data.Chain
import cats.implicits._
import io.circe.Decoder

object ArticlesExt {

  def fromChain(articlesChain: Chain[Article]): Articles = Articles(
    articlesChain.toList
  )

  def hydrate(events: Chain[EventData]): Articles =
    fromChain(events.foldLeft(Chain.empty[Article]) { (aggregate, eventData) =>
      SourceEventExt.decode(eventData).fold(aggregate) { event =>
        fold(aggregate, event)
      }
    })

  def fold(
      aggregate: Chain[Article],
      event: SourceEvent
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
