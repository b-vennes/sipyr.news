package news.sipyr.sourcing

import cats.data.Chain
import news.sipyr.events.{RSSLocation, SourceEvent, SourceLocation}
import news.sipyr.eventstore.EventData
import cats.implicits._
import news.sipyr.sourcing.Source.Location.RSS
import news.sipyr.eventstore.SourceEventExt

final case class Source(
    location: Source.Location,
    articles: Chain[Article]
)

object Source {
  enum Location {
    case RSS(url: String)
  }

  def hydrate(from: Chain[EventData]): Option[Source] =
    from
      .map(SourceEventExt.decode)
      .foldLeft(none[Source])((aggregate, maybeEvent) =>
        maybeEvent.fold(aggregate)(fold(aggregate, _))
      )

  def fold(maybeSource: Option[Source], event: SourceEvent): Option[Source] =
    event match {
      case SourceEvent.InitializedCase(initialized) =>
        Some(
          initialized.location match {
            case SourceLocation.RssCase(RSSLocation(url)) =>
              Source(Source.Location.RSS(url), Chain.empty)
          }
        )
      case SourceEvent.ArticlesAddedCase(articlesAdded) =>
        maybeSource
          .map(source =>
            source.copy(articles =
              source.articles
                .concat(
                  Chain.fromSeq(
                    articlesAdded.articles
                      .map(article => Article.atLocation(article.url))
                  )
                )
                .distinct
            )
          )
    }
}
