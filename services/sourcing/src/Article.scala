package news.sipyr.sourcing

import cats.kernel.Order

opaque type Article = String

object Article {
  def atLocation(url: String): Article = url

  given Order[Article] = Order.by[Article, String](a => a)
}
