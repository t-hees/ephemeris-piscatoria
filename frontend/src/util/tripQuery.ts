import { type environmentType } from "../components/api/Trip";

export type TripSearchParams = {
  location: string,
  environment: environmentType | "",
  from: string,
  to: string,
  minCatches: string
}

export const EMPTY_SEARCH_PARAMS: TripSearchParams = { location: "", environment: "", from: "", to: "", minCatches: ""};

export function buildTripQuery(page: number, size: number, searchParams: TripSearchParams): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (searchParams.location.trim()) params.set("location", searchParams.location.trim());
  if (searchParams.environment) params.set("environment", searchParams.environment);
  if (searchParams.from) params.set("from", searchParams.from);
  if (searchParams.to) params.set("to", searchParams.to);
  if (searchParams.minCatches) params.set("minCatches", searchParams.minCatches);
  return params.toString();
}
