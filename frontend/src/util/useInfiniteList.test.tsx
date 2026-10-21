import { describe, expect, it } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import { jsonResponse, mockFetch } from '../test/mockFetch';
import { mockIntersectionObserver } from '../test/mockIntersectionObserver';
import { useInfiniteList, type ListPage } from './useInfiniteList';

type NumberPage = { numbers: number[], hasNext: boolean };

const pagePath = (page: number) => `numbers?page=${page}`;
const toPage = (dto: NumberPage): ListPage<number> => ({ items: dto.numbers, hasNext: dto.hasNext });
const noError = () => {};

function NumberList() {
  const { items, hasNext, sentinelRef } = useInfiniteList(pagePath, toPage, noError);
  return (
    <>
      <p>{items.join(",")}</p>
      {hasNext && <div ref={sentinelRef} />}
    </>
  );
}

describe("useInfiniteList", () => {
  it("loads the first page on mount and the next pages once the sentinel is visible", async () => {
    const intersect = mockIntersectionObserver();
    const fetchMock = mockFetch({
      "numbers?page=0": () => jsonResponse({ numbers: [1, 2], hasNext: true }),
      "numbers?page=1": () => jsonResponse({ numbers: [3], hasNext: false }),
    });
    render(<NumberList />);
    expect(await screen.findByText("1,2")).toBeInTheDocument();

    act(() => intersect());
    expect(await screen.findByText("1,2,3")).toBeInTheDocument();

    // No further page after the last one
    act(() => intersect());
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  });

  it("doesn't load the same page twice while it is still loading", async () => {
    const intersect = mockIntersectionObserver();
    const fetchMock = mockFetch({
      "numbers?page=0": () => jsonResponse({ numbers: [1], hasNext: true }),
      "numbers?page=1": () => jsonResponse({ numbers: [2], hasNext: true }),
    });
    render(<NumberList />);
    act(() => intersect());
    act(() => intersect());
    expect(await screen.findByText("1")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
