package news.sipyr.sourcing

import news.sipyr.events.{ArticleDefinition, EpochSeconds}

import cats.data.Chain
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import fs2.RaiseThrowable
import fs2.data.{xml as fs2xml}
import fs2.data.xml.{dom as fs2xmldom}
import fs2.data.xml.scalaXml.*

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

import scala.xml.Document
import scala.xml.Elem
import scala.xml.Node

trait FeedClient[F[_]] {
  def articles(source: Source): F[Chain[ArticleDefinition]]
}

object FeedClient {
  private class Live(client: Client[IO]) extends FeedClient[IO] {
    override def articles(source: Source): IO[Chain[ArticleDefinition]] =
      source.location match {
        case Source.Location.RSS(url) =>
          fetch(url).flatMap(parse(url, _))
      }

    def fetch(url: String): IO[String] =
      for {
        uri <- IO.fromEither(
          Uri
            .fromString(url)
            .leftMap(error => RuntimeException(error.sanitized))
        )
        body <- client.expect[String](
          Request[IO](method = Method.GET, uri = uri)
        )
      } yield body

    def parse(sourceUrl: String, xml: String): IO[Chain[ArticleDefinition]] =
      parseDocument(xml).flatMap(document =>
        document.children.collectFirst { case element: Elem => element } match {
          case Some(root) if root.label === "feed" =>
            IO.pure(parseAtom(sourceUrl, root))
          case Some(root) if root.label === "rss" =>
            IO.pure(parseRss(sourceUrl, root))
          case Some(root) =>
            IO.raiseError(
              RuntimeException(s"Unsupported feed root element: ${root.label}")
            )
          case None =>
            IO.raiseError(RuntimeException("Feed XML had no document root"))
        }
      )
  }

  def parseDocument(xmlText: String): IO[Document] =
    Stream
      .emit(xmlText)
      .covary[IO]
      .through(fs2xml.events[IO, String])
      .through(fs2xmldom.documents[IO, Document])
      .compile
      .lastOrError

  def parseRss(sourceUrl: String, root: Elem): Chain[ArticleDefinition] = {
    val channel = childElements(root, "channel").headOption
    val outlet =
      channel
        .flatMap(textOf(_, "title"))
        .filter(_.nonEmpty)
        .getOrElse(sourceUrl)

    Chain.fromSeq(
      channel
        .map(childElements(_, "item"))
        .getOrElse(Nil)
        .flatMap(item => articleFromRssItem(item, outlet).toList)
    )
  }

  def parseAtom(sourceUrl: String, root: Elem): Chain[ArticleDefinition] = {
    val outlet = textOf(root, "title").filter(_.nonEmpty).getOrElse(sourceUrl)

    Chain.fromSeq(
      childElements(root, "entry")
        .flatMap(entry => articleFromAtomEntry(entry, outlet).toList)
    )
  }

  def articleFromRssItem(
      item: Elem,
      outlet: String
  ): Option[ArticleDefinition] = {
    val url = textOf(item, "link").orElse(
      childElements(item, "guid").headOption
        .filter(guid =>
          attribute(guid, "isPermaLink").exists(_.equalsIgnoreCase("true"))
        )
        .flatMap(text)
    )
    val title = textOf(item, "title").filter(_.nonEmpty)
    val author =
      textOf(item, "author")
        .orElse(textOf(item, "creator"))
        .getOrElse(outlet)
    val date =
      textOf(item, "pubDate")
        .orElse(textOf(item, "published"))
        .orElse(textOf(item, "updated"))
        .flatMap(parseDate)

    articleFromFields(url, title, author, outlet, date)
  }

  def articleFromAtomEntry(
      entry: Elem,
      outlet: String
  ): Option[ArticleDefinition] = {
    val url = atomLink(entry)
    val title = textOf(entry, "title").filter(_.nonEmpty)
    val author =
      childElements(entry, "author").headOption
        .flatMap(textOf(_, "name"))
        .getOrElse(outlet)
    val date =
      textOf(entry, "published")
        .orElse(textOf(entry, "updated"))
        .flatMap(parseDate)

    articleFromFields(url, title, author, outlet, date)
  }

  def articleFromFields(
      url: Option[String],
      title: Option[String],
      author: String,
      outlet: String,
      date: Option[EpochSeconds]
  ): Option[ArticleDefinition] =
    for {
      articleUrl <- url.map(_.trim).filter(_.nonEmpty)
      articleTitle <- title
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(Some(articleUrl))
      articleDate <- date.orElse(Some(EpochSeconds(0L)))
    } yield ArticleDefinition(
      id = stableArticleID(articleUrl),
      name = articleTitle,
      author = author.trim,
      outlet = outlet.trim,
      url = articleUrl,
      date = articleDate
    )

  def stableArticleID(url: String): Long = {
    val bytes = MessageDigest
      .getInstance("SHA-256")
      .digest(url.getBytes(StandardCharsets.UTF_8))
    bytes.take(8).foldLeft(0L) { (aggregate, byte) =>
      (aggregate << 8) | (byte & 0xffL)
    } & Long.MaxValue
  }

  def parseDate(value: String): Option[EpochSeconds] = {
    val normalized = value.trim
    val parsers = List(
      () => OffsetDateTime.parse(normalized).toEpochSecond,
      () =>
        ZonedDateTime
          .parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME)
          .toEpochSecond,
      () => ZonedDateTime.parse(normalized).toEpochSecond
    )

    parsers.collectFirstSome { parse =>
      Either.catchNonFatal(parse()).toOption.map(EpochSeconds(_))
    }
  }

  def atomLink(entry: Elem): Option[String] =
    childElements(entry, "link")
      .find(link =>
        attribute(link, "rel").forall(rel => rel.isBlank || rel == "alternate")
      )
      .orElse(childElements(entry, "link").headOption)
      .flatMap(link => attribute(link, "href").orElse(text(link)))

  def textOf(element: Elem, name: String): Option[String] =
    childElements(element, name).headOption.flatMap(text)

  def attribute(element: Elem, name: String): Option[String] =
    element.attributes.asAttrMap.get(name).filter(_.nonEmpty)

  def text(element: Node): Option[String] =
    Option(element.text).map(_.trim).filter(_.nonEmpty)

  def childElements(element: Node, name: String): List[Elem] =
    element.child.collect {
      case child: Elem if child.label === name => child
    }.toList

  def live(client: Client[IO]): FeedClient[IO] = new Live(client)
}
