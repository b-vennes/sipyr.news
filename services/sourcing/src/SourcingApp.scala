package news.sipyr.sourcing

import news.sipyr.eventstore.EventStreams

import cats.effect.{IO, IOApp}
import natchez.Trace.Implicits.noop
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import scala.concurrent.duration._

object SourcingApp extends IOApp.Simple {
  private val logger = Slf4jLogger.getLogger[IO]

  private val databaseHost: String =
    sys.env.getOrElse("POSTGRES_HOST", "localhost")
  private val databasePort: Int =
    sys.env.get("POSTGRES_PORT").flatMap(_.toIntOption).getOrElse(5432)
  private val databaseUser: String =
    sys.env.getOrElse("POSTGRES_USER", "postgres")
  private val databaseName: String =
    sys.env.getOrElse("POSTGRES_DATABASE", "postgres")
  private val databasePassword: String =
    sys.env.getOrElse("POSTGRES_PASSWORD", "pass")
  private val pollInterval: FiniteDuration =
    sys.env
      .get("SOURCING_POLL_INTERVAL_SECONDS")
      .flatMap(_.toLongOption) match {
      case Some(seconds) => seconds.seconds
      case None          => 5.minutes
    }

  override def run: IO[Unit] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        for {
          _ <- logger.info(
            s"Starting sourcing service with poll interval $pollInterval"
          )
          eventStreams = EventStreams.usingSkunk(
            Session.single[IO](
              host = databaseHost,
              port = databasePort,
              user = databaseUser,
              database = databaseName,
              password = Some(databasePassword)
            )
          )
          sources = Sources.usingEventStreams(eventStreams)
          service = SourcingService.create(sources, FeedClient.live(client))
          initialState <- SourcingService.initialState(sources)
          _ <- service.runForever(pollInterval).runA(initialState)
        } yield ()
      }
}
