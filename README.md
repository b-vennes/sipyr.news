# Sipyr News

Sipyr News is an open RSS feed that lets users create curated feeds and share
them with friends.

Every feed comes with a front-page of all the stories from the last twenty-four
hours.

Authentication still needs to be implemented, but it will allow users to define
their own feeds and tweak which sources are included in them.

Unauthenticated users can access and read any feeds but are not allowed to make
changes or edits. Maybe private feeds can be created later on, we'll see.

Also TODO, users should be able to see the list of sources on a feed, view all
articles beyond the front-page, and search for articles.

## Web App

The `app` directory contains a [Deno Fresh](https://fresh.deno.dev/)
application. The web app runs as a single web service which serves up pages
defined in the `app/routes` directory.

### Pages

#### Feed Front Page

The `/feed/[name]/front-page` endpoint (where name is the name of a feed)
returns a the front page for the feed. As mentioned before, the front page
contains a list of stories from the last twenty-four hours sorted by the time
they were published in descending order.

### BFF API

The BFF (backend-for-frontend) API is a local proxy which routes requests to the
implementation backend service.

#### Queries: Front Page Articles

The `/api/queries/front-page-articles` endpoint services stored article data for
a feed's front page.

The query expects the following structure:

```json
{
  "feedName": "my_feed_name",
  "initialized": {
    "secondsSinceEpoch": 1772917040
  },
  "page": 1,
  "pageSize": 10
}
```

The `initialized` field defines the time at which the request was made. This
ensures updates to the database doesn't break the paging structure. With the
backend being supported by an events database, we can retrieve any events at or
prior to the initialized time and be confident that no unexpected changes will
arrive.

## Events Service

The events project (located in `services/events`) contains event definitions
that will be stored in the `events` table in PostgreSQL. Each event belongs in a
category which can all be stored in the same stream.

There are currently two categories of events: `sources` and `feeds`.

Sources defines events related to the articles that have been published for a
particular RSS source. A source represents a website or blog which has its own
feed of articles and stories.

Feeds defines events related to the addition or removal of sources from a public
feed. A feed also has a single maintainer (initially the creator of the feed).
The maintainer role can be transferred to another user by the current
maintainer. Currently only one maintainer exists on a feed at a time. The
current maintainer may add or remove sources from a feed.

## Queries Service

The queries service (located in `services/queries`) is a Scala web backend
project using [Smithy4s](https://disneystreaming.github.io/smithy4s/) and
[Http4s](https://http4s.org/). The service reads event data from the PostgreSQL
database and aggregates the event content into views for the frontend.

A view for a frontend or client is "hydrated" using a set of events, possibly
from many streams and stream types (aka categories).
