$version: "2.0"

namespace news.sipyr.events

long MaintainerID

string SourceID

list SourceIDs {
  member: SourceID
}

structure EpochSeconds {
  @required
  secondsSinceEpoch: Long
}

structure RSSLocation {
  @required
  url: String
}

union SourceLocation {
  rss: RSSLocation
}

structure ArticleDefinition {
  @required
  id: Long

  @required
  name: String

  @required
  author: String

  @required
  outlet: String

  @required
  url: String

  @required
  date: EpochSeconds
}

list ArticleDefinitions {
  member: ArticleDefinition
}
