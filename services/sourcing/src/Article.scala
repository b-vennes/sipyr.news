package news.sipyr.sourcing

opaque type Article = String

object Article {
  def atLocation(url: String): Article = url
}
