package news.sipyr.queries

import cats.implicits.*
import cats.kernel.Order

given epochSecondsOrderInstance: Order[EpochSeconds] =
  Order.by[EpochSeconds, Long](epochSeconds => epochSeconds.secondsSinceEpoch)
