import { Option } from "effect";
import { parseNumberField } from "@/app/domains/Parsing.ts";

export interface EpochSeconds {
  secondsSinceEpoch: number;
}

export function isEpochSeconds(value: unknown): value is EpochSeconds {
  const asValid = value as EpochSeconds;
  const secondsSinceEpoch = asValid?.secondsSinceEpoch;

  return typeof secondsSinceEpoch === "number";
}

export function now(): EpochSeconds {
  const current = new Date();
  return {
    secondsSinceEpoch: Math.ceil(current.getTime() / 1000),
  };
}

export function parseEpochSeconds(value: unknown): Option.Option<EpochSeconds> {
  return Option.fromNullable(value as EpochSeconds).pipe(
    Option.flatMap((nonNullValue) =>
      Option.all({
        secondsSinceEpoch: parseNumberField(nonNullValue, "secondsSinceEpoch")
      })
    )
  )
}
