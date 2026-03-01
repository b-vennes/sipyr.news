import { define } from "@/app/utils.ts";
import FrontPageArticles from "@/app/islands/FrontPageArticles.tsx";

export default define.page(function FeedFrontPage(props) {
  const feedName = props.params.name;
  const baseUrl = props.url.origin;

  return (
    <div className="flex flex-col items-center m-1 min-h-100">
      <div className="flex flex-col justify-start items-stretch
          w-full md:w-3/4 m-4 rounded">
        <div className="flex flex-row justify-stretch">
          <p className="px-4
            text-xl font-serif
            border-l-1 border-t-1 border-r-1
            rounded-tl rounded-tr
            bg-amber-100
            ">
            Front Page
          </p>
        </div>
        <div className="
          border-l-1 border-r-1 border-b-1
          rounded-bl rounded-br
          ">
          <FrontPageArticles
            baseUrl={baseUrl}
            feedName={feedName}
          />
        </div>
      </div>
    </div>
  );
});
