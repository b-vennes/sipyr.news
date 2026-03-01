interface Props {
  name: string;
  author: string;
  outlet: string;
  date: string;
  url: string;
}

export default function ArticleListing(props: Props) {
  return (
    <a
      className="
        border border-l-4 border-b-4 border-amber-400 rounded
        flex flex-row justify-between
        bg-amber-100 hover:bg-amber-200
        px-4
        py-1
      "
      href={props.url}
      target="_blank"
    >
      <div className="flex flex-col justify-between">
        <p className="font-serif">{props.name}</p>
        <p className="font-sm">{props.author} - {props.outlet}</p>
      </div>
      <div className="flex flex-col">
        <p className="font-mono">{props.date}</p>
      </div>
    </a>
  );
}
