package news.sipyr.queries

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{port, host}
import org.http4s.ember.server.EmberServerBuilder

object Main extends IOApp.Simple {
  def run = Routes.all
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }
    .use(_ => IO.never)
}
