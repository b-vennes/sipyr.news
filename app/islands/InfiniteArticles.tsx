import type { Article } from "@/app/articles/Article.ts";
import type { Signal } from "@preact/signals";
import ArticleListing from "@/app/components/ArticleListing.tsx";
import { useRef } from "preact/hooks";

interface Props {
  articles: Signal<Array<Article>>;
}

function printScrollInfo(scrollEvent: unknown) {
  console.log("Scroll position is " + scrollEvent?.target?.scrollTop);
}

export default function InfiniteArticles(props: Props) {
  return (
    <div
      className="
        flex flex-col items-stretch gap-2
        snap-y overflow-y-scroll
      "
      onScroll={printScrollInfo}
    >
      {props.articles.value.map((article) => (
        <ArticleListing
          key={article.id}
          name={article.name}
          author={article.author}
          outlet={article.outlet}
          url={article.url}
          date={article.date}
        />
      ))}
    </div>
  );
}
