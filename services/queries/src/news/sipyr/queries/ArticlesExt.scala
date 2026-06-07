package news.sipyr.queries

import cats.data.Chain
import news.sipyr.events.{ArticleDefinition, SourceEvent}
import news.sipyr.eventstore.{EventData, SourceEventExt}

object ArticlesExt {
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

  private def toArticle(definition: ArticleDefinition): Article =
    Article(
      id = definition.id,
      name = definition.name,
      author = definition.author,
      outlet = definition.outlet,
      url = definition.url,
      date = EpochSeconds(definition.date.secondsSinceEpoch)
    )

  private def fromChain(articlesChain: Chain[Article]): Articles = Articles(
    articlesChain.toList
  )
}
