import { describe, expect, it } from 'vitest';
import { buildCatchQuery, EMPTY_CATCH_SEARCH_PARAMS } from './catchQuery';

describe("buildCatchQuery", () => {
  it("only contains paging without search params", () => {
    expect(buildCatchQuery(1, 20, EMPTY_CATCH_SEARCH_PARAMS)).toBe("page=1&size=20");
  });

  it("contains all set search params", () => {
    const query = new URLSearchParams(buildCatchQuery(0, 20, {
      species: " hecht ",
      location: "  cool lake ",
      from: "2026-01-01",
      to: "2026-02-01",
      minSize: "80",
      minWeight: "2000",
      withImage: true,
    }));
    expect(Object.fromEntries(query)).toEqual({
      page: "0",
      size: "20",
      species: "hecht",
      location: "cool lake",
      from: "2026-01-01",
      to: "2026-02-01",
      minSize: "80",
      minWeight: "2000",
      withImage: "true",
    });
  });

  it("omits blank text params", () => {
    expect(buildCatchQuery(0, 20, { ...EMPTY_CATCH_SEARCH_PARAMS, species: "  ", location: " " }))
      .toBe("page=0&size=20");
  });
});
