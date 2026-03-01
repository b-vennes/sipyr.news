$version: "2.0"

namespace news.sipyr.events

long FeedID

long MaintainerID

long SourceID

list SourceIDs {
  member: SourceID
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
  url: String

  @required
  date: String
}

list ArticleDefinitions {
  member: ArticleDefinition
}
