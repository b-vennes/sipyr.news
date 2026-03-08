package news.sipyr.queries

import cats.data.Chain
import scala.annotation.targetName

object ArticlesExt {
  def fromChain(articlesChain: Chain[Article]): Articles = Articles(
    articlesChain.toList
  )

  def hydrate(events: Chain[EventData]): Articles = Articles(List.empty)
}
