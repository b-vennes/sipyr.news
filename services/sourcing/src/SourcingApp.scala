package news.sipyr.sourcing

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object SourcingApp extends IOApp.Simple {
  val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = logger.info("hello!")
}
