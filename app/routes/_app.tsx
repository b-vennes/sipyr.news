import { define } from "../utils.ts";

export default define.page(function App({ Component }) {
  return (
    <html>
      <head>
        <meta charset="utf-8"/>
        <title>Sipyr News</title>
      </head>
      <body>
        <div className="md:m-2 md:rounded-2xl bg-linear-to-bl from-violet-500 to-orange-100">
          <Component/>
        </div>
      </body>
    </html>
  );
});
