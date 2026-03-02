package news.sipyr.queries

import cats.data.Chain

trait EventStreams[F[_]] {
  def read(eventStream: EventStream, time: EpochSeconds): F[Chain[EventData]]

  def readMany(eventStreams: Chain[EventStream], time: EpochSeconds): F[Chain[EventData]]
}
