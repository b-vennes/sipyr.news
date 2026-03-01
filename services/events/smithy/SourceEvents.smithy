$version: "2.0"

namespace news.sipyr.events

structure SourceInitialized {
  @required
  id: SourceID

  @required
  location: SourceLocation
}

structure ArticlesAdded {
  @required
  articles: ArticleDefinitions
}

union SourceEvent {
  initialized: SourceInitialized
  articlesAdded: ArticlesAdded
}
