package news.sipyr.testutils

import cats.data.Chain
import cats.effect.std.Env
import cats.effect.{IO, Resource}
import cats.implicits.*
import natchez.Trace.Implicits.noop
import news.sipyr.eventstore.EventStreams
import skunk.codec.all.*
import skunk.implicits.*
import skunk.{Command, Session}

import java.net.ConnectException

object Database {
  private val databaseHost: IO[String] =
    Env[IO].get("TEST_POSTGRES_HOST").map(_.getOrElse("localhost"))
  private val databasePort: IO[Int] =
    Env[IO]
      .get("TEST_POSTGRES_PORT")
      .map(_.flatMap(_.toIntOption).getOrElse(5432))
  private val databaseUser: IO[String] =
    Env[IO].get("TEST_POSTGRES_USER").map(_.getOrElse("postgres"))
  private val databaseName: IO[String] =
    Env[IO].get("TEST_POSTGRES_DATABASE").map(_.getOrElse("postgres"))
  private val databasePassword: IO[String] =
    Env[IO].get("TEST_POSTGRES_PASSWORD").map(_.getOrElse("pass"))

  val session: Resource[IO, Session[IO]] = for {
    databaseHost <- Resource.eval(databaseHost)
    databasePort <- Resource.eval(databasePort)
    databaseUser <- Resource.eval(databaseUser)
    databaseName <- Resource.eval(databaseName)
    databasePassword <- Resource.eval(databasePassword)
    session <- Session.single[IO](
      host = databaseHost,
      port = databasePort,
      user = databaseUser,
      database = databaseName,
      password = Some(databasePassword)
    )
  } yield session

  def ensureAvailable: IO[Unit] =
    session
      .use(
        _.prepare(sql"select 1".query(int4)).void
      )
      .handleErrorWith {
        case error if isConnectionRefused(error) =>
          IO.println(
            "Connection refused while running database integration tests. Start the root docker compose environment before running tests."
          ) *> IO.raiseError(error)
        case error =>
          IO.raiseError(error)
      }

  def isConnectionRefused(error: Throwable): Boolean =
    error.isInstanceOf[ConnectException] ||
      error.getMessage.contains("Connection refused")

  def insertEventRows(rows: Chain[EventStreams.EventRow]): IO[Unit] =
    session.use { s =>
      s.prepare(EventStreams.insertEvent)
        .flatMap(command =>
          rows.traverse_(row =>
            command
              .execute(
                (
                  row.persistedAt,
                  row.typeName,
                  row.streamName,
                  row.index,
                  row.eventTypeName,
                  row.content
                )
              )
              .void
          )
        )
    }

}
