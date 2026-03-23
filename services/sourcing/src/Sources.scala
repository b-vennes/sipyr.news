package news.sipyr.sourcing

import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  EpochSeconds,
  SourceID
}
import news.sipyr.eventstore.{
  EventData,
  EventStream,
  EventStreams,
  SourceEventExt,
  toEventStreamID
}

import cats.data.{Chain, NonEmptyChain}
import cats.effect.IO
import cats.implicits._
import io.circe.Json

final case class StoredSource(
    id: SourceID,
    source: Source,
    nextIndex: Int
)

trait Sources[F[_]] {
  def all: F[Chain[StoredSource]]

  def withID(id: SourceID): F[StoredSource]

  def addArticles(
      id: SourceID,
      expectedIndex: Int,
      persistedAt: EpochSeconds,
      articles: NonEmptyChain[ArticleDefinition]
  ): F[Unit]
}

object Sources {
  private class UsingEventStreams(eventStreams: EventStreams[IO])
      extends Sources[IO] {

    override def all: IO[Chain[StoredSource]] = for {
      now <- IO.realTime
      sourceIDs <- eventStreams.streamNames(
        EventStream.Categories.sources,
        EpochSeconds(now.toSeconds)
      )
      sources <- sourceIDs.traverse(streamID =>
        withID(SourceID(streamID.streamName))
      )
    } yield sources

    override def withID(id: SourceID): IO[StoredSource] = for {
      now <- IO.realTime
      events <- eventStreams.read(
        EventStream(
          id.toEventStreamID,
          EventStream.Categories.sources
        ),
        EpochSeconds(now.toSeconds)
      )
      source <- IO.fromOption(Source.hydrate(events))(
        RuntimeException(s"Source ${id.value} could not be hydrated")
      )
    } yield StoredSource(
      id = id,
      source = source,
      nextIndex = events.length.toInt
    )

    override def addArticles(
        id: SourceID,
        expectedIndex: Int,
        persistedAt: EpochSeconds,
        articles: NonEmptyChain[ArticleDefinition]
    ): IO[Unit] =
      eventStreams.append(
        EventStream(
          id.toEventStreamID,
          EventStream.Categories.sources
        ),
        persistedAt = persistedAt,
        index = expectedIndex,
        eventTypeName = SourceEventExt.articlesAddedEventType,
        content = EventData.Content.fromJson(encodeArticlesAdded(articles))
      )
  }

  private def encodeArticlesAdded(
      articles: NonEmptyChain[ArticleDefinition]
  ): Json =
    Json.obj(
      "articles" -> Json.arr(articles.toList.map(articleToJson)*)
    )

  private def articleToJson(article: ArticleDefinition): Json =
    Json.obj(
      "id" -> Json.fromLong(article.id),
      "name" -> Json.fromString(article.name),
      "author" -> Json.fromString(article.author),
      "outlet" -> Json.fromString(article.outlet),
      "url" -> Json.fromString(article.url),
      "date" -> Json.obj(
        "secondsSinceEpoch" -> Json.fromLong(article.date.secondsSinceEpoch)
      )
    )

  def usingEventStreams(eventStreams: EventStreams[IO]): Sources[IO] =
    new UsingEventStreams(eventStreams)
}
