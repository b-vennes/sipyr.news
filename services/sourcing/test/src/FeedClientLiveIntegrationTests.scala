package news.sipyr.sourcing

import cats.effect.IO
import cats.implicits._
import munit.CatsEffectSuite
import org.http4s.ember.client.EmberClientBuilder

class FeedClientLiveIntegrationTests extends CatsEffectSuite {
  test("live client parses articles from the Remap Radio RSS feed") {
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        val underTest = FeedClient.live(client)

        underTest
          .articles(Source(Source.Location.RSS("https://remapradio.com/rss/"), cats.data.Chain.empty))
          .map { articles =>
            assert(articles.nonEmpty, "expected the live RSS feed to contain at least one article")
            assert(
              articles.forall(article =>
                article.name.nonEmpty &&
                  article.author.nonEmpty &&
                  article.outlet.nonEmpty &&
                  article.url.nonEmpty &&
                  article.date.secondsSinceEpoch > 0
              ),
              "expected all parsed articles to include complete metadata"
            )
          }
      }
  }
}
