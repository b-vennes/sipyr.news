import { Article, parseArticle } from "@/app/domains/articles/Article.ts";
import { EpochSeconds } from "@/app/domains/shared/EpochSeconds.ts";
import {
  parseQueryResponse,
  QueryRequest,
  QueryResponse,
} from "@/app/routes/api/queries/front-page-articles.ts";

import { Effect, Option } from "effect";

export interface FeedPage {
  pageNumber: number;
  pageSize: number;
  articles: Array<Article>;
}

export interface FeedNotFound {
  feedName: string;
}

export interface FeedUnavailable {
  reason: string;
}

export interface Feeds {
  frontPage(
    name: string,
    initialized: EpochSeconds,
    page: number,
    pageSize: number,
  ): Effect.Effect<FeedPage, FeedNotFound | FeedUnavailable, never>;
}

export class APIFrontPage implements Feeds {
  readonly baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  frontPage(
    name: string,
    initialized: EpochSeconds,
    page: number,
    pageSize: number,
  ): Effect.Effect<FeedPage, FeedNotFound | FeedUnavailable, never> {
    const request: QueryRequest = {
      feedName: name,
      initialized,
      page,
      pageSize,
    };

    return Effect.promise(() =>
      fetch(
        this.baseUrl + "/api/queries/front-page-articles",
        {
          method: "POST",
          body: JSON.stringify(request),
        },
      )
    ).pipe(
      Effect.flatMap((response) => Effect.promise(() => response.json())),
    ).pipe(
      Effect.map((responseObj) => parseQueryResponse(responseObj)),
    ).pipe(
      Effect.flatMap((parsed) =>
        Option.match(parsed, {
          onSome: (response) =>
            Effect.succeed({
              pageNumber: page,
              pageSize: pageSize,
              articles: response.articles,
            }),
          onNone: () =>
            Effect.fail(
              {
                reason: "Failed to parse response value from the API.",
              },
            ),
        })
      ),
    );
  }
}
