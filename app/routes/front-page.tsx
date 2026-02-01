import { define } from "../utils.ts";
import InfiniteArticles from "../islands/InfiniteArticles.tsx";
import type { Article } from "@/app/articles/Article.ts";
import { type Signal, useSignal } from "@preact/signals";

export default define.page(function FrontPage(_ctx) {
  const articles: Signal<Array<Article>> = useSignal([
    {
      id: 1,
      name: "Moltbot Is Taking Over Silicon Valley",
      author: "Will Knight",
      url:
        "https://www.wired.com/story/clawdbot-moltbot-viral-ai-assistant/#intcid=_wired-verso-hp-trending_225b8df4-249b-426c-b1c0-116a8730a819_popular4-2",
      outlet: "Wired",
      date: "01/28/26",
    },
    {
      id: 2,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
    {
      id: 3,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
    {
      id: 4,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
    {
      id: 5,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
    {
      id: 6,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
    {
      id: 7,
      name:
        "An AI Toy Exposed 50,000 Logs of Its Chats With Kids to Anyone With a Gmail Account",
      author: "Andy Greenberg",
      outlet: "Wired",
      date: "01/29/26",
      url:
        "https://www.wired.com/story/an-ai-toy-exposed-50000-logs-of-its-chats-with-kids-to-anyone-with-a-gmail-account/",
    },
  ]);

  return (
    <div className="flex flex-col items-center m-1">
      <div className="flex flex-col justify-start items-stretch
          w-full md:w-1/2 h-96">
        <InfiniteArticles articles={articles} />
      </div>
    </div>
  );
});
