import { define } from "../../../utils.ts";
import { EpochSeconds } from "@/app/domains/shared/EpochSeconds.ts";
import { Article, parseArticle } from "@/app/domains/articles/Article.ts";
import { Cause, Effect, Exit, Option } from "effect";
import {
  parseListField,
  parseNumberField,
  parseStringField,
} from "@/app/domains/Parsing.ts";

const queriesAPI = "http://localhost:9000"

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
        Effect.tap((request) =>
          Effect.log(`Received request '${JSON.stringify(request)}'.`)
        ),
      )
      .pipe(
        Effect.flatMap((request) =>
          Effect
            .tryPromise({
              try: () => fetch(
                queriesAPI + `/front-page/${request.feedName}`,
                {
                  method: "POST",
                  body: JSON.stringify({
                    page: request.page,
                    pageSize: request.pageSize,
                    initialized: {
                      secondsSinceEpoch: request.initialized.secondsSinceEpoch
                    }
                  })
                }
              ),
              catch: (unknownError) => `Failed to call the queries API: ${unknownError}`
            })
        ),
      )
      .pipe(
        Effect.flatMap((response) =>
          Effect.tryPromise({
            try: () => response.json(),
            catch: (unknownError) => `Failed to read response body from the call to the queries API: ${unknownError}`
          })
        ),
      )
      .pipe(
        Effect.flatMap((responseBody) =>
          Option.match(parseQueryResponse(responseBody), {
            onNone: () => Effect.fail(`Failed to parse the response from the queries API: ${JSON.stringify(responseBody)}`),
            onSome: (decoded) => Effect.succeed(decoded)
          })
        ),
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
