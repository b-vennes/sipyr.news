package news.sipyr.queries

import news.sipyr.events.SourceIDs

import cats.data.Chain
import scala.annotation.targetName

object SourceIDsExt {
  def hydrate(events: Chain[EventData]): SourceIDs = SourceIDs(List.empty)
}
