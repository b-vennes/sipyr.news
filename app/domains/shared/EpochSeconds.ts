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
    secondsSinceEpoch: current.getTime() / 1000,
  };
}
