package news.sipyr.sourcing

trait Sources[F[_]] {
  def withID(id: Source.ID): F[Source]
}
