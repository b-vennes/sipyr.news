import { Feeds } from "@/app/domains/feeds/Feeds.ts";
import { Effect } from "effect";
import { mockArticles } from "@/app/mock/Articles.ts";

export function mockFeeds(): Feeds {
  return {
    frontPage: (_name, _initialized, pageNumber, pageSize) =>
      Effect.succeed({
        pageNumber: pageNumber,
        pageSize: pageSize,
        articles: mockArticles,
      }),
  };
}
