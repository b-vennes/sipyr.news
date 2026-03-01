$version: "2.0"
namespace news.sipyr.events

structure FeedCreated {
  @required
  id: FeedID

  @required
  maintainer: MaintainerID

  @required
  name: String

  @required
  sources: SourceIDs
}

structure SourcesAdded {
  @required
  sources: SourceIDs
}

structure SourcesRemoved {
  @required
  sources: SourceIDs
}

structure MaintainerChanged {
  @required
  maintainer: MaintainerID
}

union FeedEvent {
  created: FeedCreated
  sourcesAdded: SourcesAdded
  sourcesRemoved: SourcesRemoved
  maintainerChanged: MaintainerChanged
}
