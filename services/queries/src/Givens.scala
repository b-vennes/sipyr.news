package news.sipyr.queries

import cats.kernel.Order
import cats.implicits._

given epochSecondsOrderInstance: Order[EpochSeconds] =
  Order.by[EpochSeconds, Long](epochSeconds => epochSeconds.secondsSinceEpoch)
