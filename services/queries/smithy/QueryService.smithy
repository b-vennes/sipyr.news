$version: "2.0"
namespace news.sipyr.queries

use alloy#simpleRestJson

@simpleRestJson
service QueryService {
  version: "0.0.1",
  operations: [FrontPage]
}

@http(method: "POST", uri: "/front-page/{feedName}", code: 200)
operation FrontPage {
  input: FrontPageRequest
  output: FrontPageResponse
}

structure Article {
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
  date: String
}

list Articles {
  member: Article
}

structure EpochSeconds {
  @required
  secondsSinceEpoch: Long
}

structure FrontPageRequest {
  @httpLabel
  @required
  feedName: String,

  @required
  page: Integer,

  @required
  pageSize: Integer,

  @required
  initialized: EpochSeconds
}

structure FrontPageResponse {
  @required
  articles: Articles
}
