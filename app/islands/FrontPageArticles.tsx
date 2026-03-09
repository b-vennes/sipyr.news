import type { Article } from "@/app/domains/articles/Article.ts";
import ArticleListing from "@/app/components/ArticleListing.tsx";
import { Effect } from "effect";
import {
  type Signal,
  useComputed,
  useSignal,
  useSignalEffect,
} from "@preact/signals";
import { now } from "@/app/domains/shared/EpochSeconds.ts";
import { APIFrontPage } from "../domains/feeds/Feeds.ts";

interface Props {
  feedName: string;
  baseUrl: string;
}

export default function FrontPageArticles(props: Props) {
  const feeds = new APIFrontPage(props.baseUrl);

  const initialized = useSignal(now());
  const page = useSignal(1);

  const articles: Signal<Array<[number, Array<Article>]>> = useSignal(
    [],
  );

  useSignalEffect(() => {
    const currentInitialized = initialized.value;
    const currentPage = page.value;

    Effect.runFork(
      feeds.frontPage(props.feedName, currentInitialized, currentPage, 10)
        .pipe(
          Effect.tap((feed) => {
            const current = articles.peek();

            articles.value = current.some((value) =>
                value[0] === feed.pageNumber
              )
              ? current.map((value) =>
                value[0] === feed.pageNumber
                  ? [feed.pageNumber, feed.articles]
                  : value
              )
              : [...current, [feed.pageNumber, feed.articles]];
          }),
        )
        .pipe(
          Effect.onError((error) => Effect.logError(error.toString())),
        ),
    );
  });

  const onMoreClicked = () => {
    page.value = page.value + 1;
  };

  const allArticles = useComputed(() => {
    return articles.value.sort((a, b) => a[0] - b[0]).flatMap((value) =>
      value[1]
    );
  });

  return (
    <div className="
        flex flex-col items-stretch gap-2
        bg-amber-100
        p-2
        rounded
      ">
      {allArticles.value.map((article) => (
        <ArticleListing
          key={article.id}
          name={article.name}
          author={article.author}
          outlet={article.outlet}
          url={article.url}
          date={article.date}
        />
      ))}
      <div>
        <button
          className="border rounded bg-amber-100 hover:cursor-pointer px-3 py-1 ml-4"
          type="button"
          onClick={onMoreClicked}
        >
          More
        </button>
      </div>
    </div>
  );
}
