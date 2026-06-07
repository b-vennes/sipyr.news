package news.sipyr.queries

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port, host, port}
import natchez.Trace.Implicits.noop
import news.sipyr.eventstore.EventStreams
import org.http4s.ember.server.EmberServerBuilder
import skunk.Session

object Main extends IOApp.Simple {
  private val serviceHost: String =
    sys.env.getOrElse("QUERIES_HOST", "0.0.0.0")
  private val servicePort: Int =
    sys.env.get("QUERIES_PORT").flatMap(_.toIntOption).getOrElse(9000)
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
          .withPort(Port.fromInt(servicePort).getOrElse(port"9000"))
          .withHost(Host.fromString(serviceHost).getOrElse(host"0.0.0.0"))
          .withHttpApp(routes.orNotFound)
          .build
      }
      .use(_ => IO.never)
  } yield ()
}
