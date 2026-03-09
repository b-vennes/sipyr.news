import { Option } from "effect";
import { parseNumberField, parseStringField } from "@/app/domains/Parsing.ts";
import { EpochSeconds, parseEpochSeconds } from "@/app/domains/shared/EpochSeconds.ts";

export interface Article {
  id: number;
  name: string;
  author: string;
  outlet: string;
  url: string;
  date: EpochSeconds;
}

export function parseArticle(maybeArticle: unknown): Option.Option<Article> {
  return Option.fromNullable(maybeArticle).pipe(
    Option.flatMap((nonNullMaybeArticle) =>
      Option.all({
        id: parseNumberField(nonNullMaybeArticle, "id"),
        name: parseStringField(nonNullMaybeArticle, "name"),
        author: parseStringField(nonNullMaybeArticle, "author"),
        outlet: parseStringField(nonNullMaybeArticle, "outlet"),
        url: parseStringField(nonNullMaybeArticle, "url"),
        date: parseEpochSeconds(
          (nonNullMaybeArticle as { date: unknown})?.date
        ),
      })
    ),
  );
}
