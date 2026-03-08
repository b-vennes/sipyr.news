package news.sipyr.queries

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{port, host}
import org.http4s.ember.server.EmberServerBuilder
import skunk.Session
import natchez.Trace.Implicits.noop

object Main extends IOApp.Simple {
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

  def run = for {
    eventStreams = EventStreams.usingSkunk(
      Session.single[IO](
        host = databaseHost,
        port = databasePort,
        user = databaseUser,
        database = databaseName,
        password = Some(databasePassword)
      )
    )
    feeds = Feeds.usingEventStreams(eventStreams)
    sources = Sources.usingEventStreams(eventStreams)
    _ <- Routes
      .all(feeds, sources)
      .flatMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withPort(port"9000")
          .withHost(host"localhost")
          .withHttpApp(routes.orNotFound)
          .build
      }
      .use(_ => IO.never)
  } yield ()
}
