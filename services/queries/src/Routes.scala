package news.sipyr.queries

import cats.effect.{IO, Resource}
import org.http4s.{HttpRoutes}
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.implicits._

object Routes {
  def query(feeds: Feeds[IO], sources: Sources[IO]): Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(QueryServiceImpl(feeds, sources)).resource

  val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](QueryService)

  def all(feeds: Feeds[IO], sources: Sources[IO]): Resource[IO, HttpRoutes[IO]] = query(feeds, sources).map(_ <+> docs)
}
