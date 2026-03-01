import { Option } from "effect";

export function parseStringField(
  root: unknown,
  fieldName: string,
): Option.Option<string> {
  const validRoot = root as { [field: string]: string };
  const fieldValue = validRoot ? validRoot[fieldName] : null;

  return typeof fieldValue === "string"
    ? Option.some(fieldValue)
    : Option.none();
}

export function parseNumberField(
  root: unknown,
  fieldName: string,
): Option.Option<number> {
  const validRoot = root as { [field: string]: number };
  const fieldValue = validRoot ? validRoot[fieldName] : null;

  return typeof fieldValue === "number"
    ? Option.some(fieldValue)
    : Option.none();
}

export function parseListField<A>(
  root: unknown,
  fieldName: string,
  elementParsing: (element: unknown) => Option.Option<A>,
): Option.Option<Array<A>> {
  const validRoot = root as { [field: string]: Array<unknown> };
  const fieldValue = validRoot ? validRoot[fieldName] : null;

  return Array.isArray(fieldValue)
    ? Option.all(fieldValue.map(elementParsing))
    : Option.none();
}
