package news.sipyr.queries

import cats.effect.IO

object QueryServiceImpl extends QueryService[IO] {
  override def frontPage(
    feedName: String,
    page: Int,
    pageSize: Int,
    initialized: EpochSeconds): IO[FrontPageResponse] =
      IO.pure(
        FrontPageResponse(
          List(
            Article(
              1L,
              "On NVIDIA and Analyslop",
              "Ed Zitron",
              "Where's Your Ed At",
              "https://www.wheresyoured.at/on-nvidia-and-analyslop/",
              "02-26-2026"
            )
          )
        )
      )
}
