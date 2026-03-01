package news.sipyr.queries

import cats.effect.{IO, Resource}
import org.http4s.{HttpRoutes}
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.implicits._

object Routes {
  private val query: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(QueryServiceImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](QueryService)

  val all: Resource[IO, HttpRoutes[IO]] = query.map(_ <+> docs)
}
