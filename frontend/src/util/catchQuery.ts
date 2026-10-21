export type CatchSearchParams = {
  species: string,
  location: string,
  from: string,
  to: string,
  minSize: string,
  minWeight: string,
  withImage: boolean,
}

export const EMPTY_CATCH_SEARCH_PARAMS: CatchSearchParams = {
  species: "", location: "", from: "", to: "", minSize: "", minWeight: "", withImage: false
};

export function buildCatchQuery(page: number, size: number, searchParams: CatchSearchParams): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (searchParams.species.trim()) params.set("species", searchParams.species.trim());
  if (searchParams.location.trim()) params.set("location", searchParams.location.trim());
  if (searchParams.from) params.set("from", searchParams.from);
  if (searchParams.to) params.set("to", searchParams.to);
  if (searchParams.minSize) params.set("minSize", searchParams.minSize);
  if (searchParams.minWeight) params.set("minWeight", searchParams.minWeight);
  if (searchParams.withImage) params.set("withImage", "true");
  return params.toString();
}
