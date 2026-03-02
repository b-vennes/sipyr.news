package news.sipyr.queries

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{port, host}
import org.http4s.ember.server.EmberServerBuilder
import news.sipyr.events.{SourceID, SourceIDs}

object Main extends IOApp.Simple {

  val feeds: Feeds[IO] = new Feeds[IO] {
    override def sources(feedName: String, time: EpochSeconds): IO[SourceIDs] =
      IO(SourceIDs(List(
          SourceID(1),
          SourceID(2),
          SourceID(3)
      )))
  }

  val sources: Sources[IO] = new Sources[IO] {
    override def articles(
      sources: SourceIDs,
      initialized: EpochSeconds,
      from: EpochSeconds,
      to: EpochSeconds): IO[Articles] =
      IO(Articles(List(
        Article(
          1,
          "Test",
          "Test Author",
          "Source Name",
          "Source URL",
          EpochSeconds.fromStringUnsafe("03-01-2026")
        )
      )))
  }

  def run = for {
    _ <- Routes.all(feeds, sources)
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
