$version: "2.0"

namespace news.sipyr.events

structure SourceInitialized {
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
