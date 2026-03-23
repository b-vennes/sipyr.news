package news.sipyr.sourcing

import news.sipyr.events.{
  ArticleDefinition,
  ArticlesAdded,
  EpochSeconds,
  SourceID
}

import cats.data.Chain
import cats.data.NonEmptyChain
import cats.data.StateT
import cats.effect.IO
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._
import scala.util.Random

final case class SourcingState(
    sources: Map[SourceID, StoredSource]
)

class SourcingService(
    sources: Sources[IO],
    feedClient: FeedClient[IO]
) {
  private val logger = Slf4jLogger.getLogger[IO]
  private val random = Random()

  def runForever(interval: FiniteDuration): StateT[IO, SourcingState, Unit] =
    for {
      discovered <- pollOnce
      _ <- StateT.liftF(
        logger.info(
          s"Completed sourcing pass with ${discovered.length} emitted source events"
        )
      )
      _ <- StateT.liftF(IO.sleep(interval))
      _ <- runForever(interval)
    } yield ()

  def pollOnce: StateT[IO, SourcingState, Chain[(SourceID, ArticlesAdded)]] =
    for {
      current <- StateT.get[IO, SourcingState]
      sourceIDs <- StateT.liftF[IO, SourcingState, Chain[SourceID]](
        IO.delay(Chain.fromSeq(random.shuffle(current.sources.keys.toList)))
      )
      discovered <- sourceIDs.traverseFilter(processSource)
    } yield discovered

  def processSource(
      sourceID: SourceID
  ): StateT[IO, SourcingState, Option[(SourceID, ArticlesAdded)]] =
    StateT { current =>
      current.sources.get(sourceID) match {
        case None =>
          IO.pure(current -> none[(SourceID, ArticlesAdded)])
        case Some(storedSource) =>
          feedClient
            .articles(storedSource.source)
            .flatMap { fetchedArticles =>
              val knownArticles = storedSource.source.articles.toList.toSet
              val newArticles = fetchedArticles.filterNot(article =>
                knownArticles.contains(Article.atLocation(article.url))
              )

              NonEmptyChain
                .fromChain(newArticles)
                .fold(
                  IO.pure(current -> none[(SourceID, ArticlesAdded)])
                )(emitArticlesAdded(current, storedSource, _))
            }
            .handleErrorWith { error =>
              logger.error(error)(
                s"Failed to source articles for source='${sourceID.value}'."
              ) *> IO.pure(current -> none[(SourceID, ArticlesAdded)])
            }
      }
    }

  def emitArticlesAdded(
      current: SourcingState,
      storedSource: StoredSource,
      newArticles: NonEmptyChain[ArticleDefinition]
  ): IO[(SourcingState, Option[(SourceID, ArticlesAdded)])] =
    for {
      now <- IO.realTime.map(duration => EpochSeconds(duration.toSeconds))
      _ <- sources.addArticles(
        id = storedSource.id,
        expectedIndex = storedSource.nextIndex,
        persistedAt = now,
        articles = newArticles
      )
      event = ArticlesAdded(newArticles.toList)
      updatedSource = storedSource.copy(
        source = storedSource.source.copy(
          articles = (
            storedSource.source.articles ++
              Chain.fromSeq(
                newArticles.toList.map(article =>
                  Article.atLocation(article.url)
                )
              )
          ).distinct
        ),
        nextIndex = storedSource.nextIndex + 1
      )
      _ <- logger.info(
        s"Emitted ${newArticles.length} new article definitions for source=${storedSource.id.value}"
      )
    } yield (
      current.copy(
        sources = current.sources.updated(storedSource.id, updatedSource)
      ) -> Some(storedSource.id -> event)
    )
}

object SourcingService {
  def initialState(sources: Sources[IO]): IO[SourcingState] =
    for {
      loadedSources <- sources.all
    } yield SourcingState(
      loadedSources.toList.map(source => source.id -> source).toMap
    )

  def create(
      sources: Sources[IO],
      feedClient: FeedClient[IO]
  ): SourcingService =
    new SourcingService(sources, feedClient)
}
