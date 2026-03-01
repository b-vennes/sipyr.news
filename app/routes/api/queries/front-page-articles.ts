import { define } from "../../../utils.ts";
import { EpochSeconds } from "@/app/domains/shared/EpochSeconds.ts";
import { Article, parseArticle } from "@/app/domains/articles/Article.ts";
import { Effect, Exit, Option } from "effect";
import {
  parseListField,
  parseNumberField,
  parseStringField,
} from "@/app/domains/Parsing.ts";

export interface QueryRequest {
  feedName: string;
  initialized: EpochSeconds;
  page: number;
  pageSize: number;
}

export interface QueryResponse {
  articles: Array<Article>;
}

function parseInitialized(
  maybeInitialized: unknown,
): Option.Option<EpochSeconds> {
  return Option.fromNullable(maybeInitialized).pipe(
    Option.flatMap((initialized) =>
      Option.all({
        secondsSinceEpoch: parseNumberField(initialized, "secondsSinceEpoch"),
      })
    ),
  );
}

export function parseQueryRequest(
  maybeRequest: unknown,
): Option.Option<QueryRequest> {
  return Option.fromNullable(maybeRequest).pipe(
    Option.flatMap((request) =>
      Option.all({
        feedName: parseStringField(request, "feedName"),
        initialized: parseInitialized(
          (request as { initialized: unknown })?.initialized,
        ),
        page: parseNumberField(request, "page"),
        pageSize: parseNumberField(request, "pageSize"),
      })
    ),
  );
}

export function parseQueryResponse(
  maybeResponse: unknown,
): Option.Option<QueryResponse> {
  return Option.fromNullable(maybeResponse).pipe(
    Option.flatMap((response) =>
      Option.all({
        articles: parseListField(response, "articles", parseArticle),
      })
    ),
  );
}

const mockArticles: Array<Article> = [
  {
    id: 1,
    name: "Premium: The Hater's Guide to Private Equity",
    author: "Ed Zitron",
    outlet: "Where's Your Ed At",
    url: "https://www.wheresyoured.at/hatersguide-pe/",
    date: "02-27-2026",
  },
  {
    id: 2,
    name: "On NVIDIA And Analyslop",
    author: "Ed Zitron",
    outlet: "Where's Your Ed At",
    url: "https://www.wheresyoured.at/on-nvidia-and-analyslop/",
    date: "02-26-2026",
  },
];

export const handler = define.handlers({
  async POST(ctx) {
    const run = Effect
      .promise(() => ctx.req.json())
      .pipe(
        Effect.flatMap((requestBody) =>
          Option.match(parseQueryRequest(requestBody), {
            onSome: (request) => Effect.succeed(request),
            onNone: () =>
              Effect.fail(
                `Failed to parse request object. Given '${
                  JSON.stringify(requestBody)
                }'.`,
              ),
          })
        ),
      )
      .pipe(
        Effect.flatMap((request) =>
          Effect.log(`Received request '${JSON.stringify(request)}'.`)
        ),
      )
      .pipe(
        Effect.map(() => {
          const responseValue: QueryResponse = {
            articles: mockArticles,
          };

          return responseValue;
        }),
      )
      .pipe(
        Effect.map((responseValue) => Response.json(responseValue)),
      );

    const result = await Effect.runPromiseExit(run);

    return Exit.match(result, {
      onFailure: (error) => {
        console.log(error);
        return Response.json({
          error: error,
        });
      },
      onSuccess: (response) => response,
    });
  },
});
