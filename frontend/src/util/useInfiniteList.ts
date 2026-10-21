import { useCallback, useEffect, useRef, useState } from "react";
import { fetchApi } from "./fetchApi";

export type ListPage<T> = { items: T[], hasNext: boolean };

/**
 * Loads a paged list page by page, the next page is loaded once the element at sentinelRef becomes visible.
 * Meant to be remounted (e.g. through a key) to start over, for example when search params change.
 * @param pagePath The relative api path of a page, has to be stable across renders (useCallback)
 * @param toPage Extracts the items from the response json, has to be stable across renders as well
 */
export function useInfiniteList<T, PageDto>(pagePath: (page: number) => string,
  toPage: (dto: PageDto) => ListPage<T>, setError: (error: string | null) => void) {

  const [items, setItems] = useState<T[]>([]);
  const [hasNext, setHasNext] = useState<boolean>(true);
  const [loading, setLoading] = useState<boolean>(false);
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const loadingRef = useRef<boolean>(false); // necessary to avoid reloading same page
  // Refs instead of state, as the observer may call loadNextPage after a page loaded but before the
  // rerender replaced its closure, which would otherwise load (and append) the same page again
  const pageRef = useRef<number>(0);
  const hasNextRef = useRef<boolean>(true);

  const loadNextPage = useCallback(() => {
    if (loadingRef.current || !hasNextRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    fetchApi(pagePath(pageRef.current), "GET",
      async (response) => {
        const page = toPage(await response.json());
        pageRef.current += 1;
        hasNextRef.current = page.hasNext;
        setItems((prev) => [...prev, ...page.items]);
        setHasNext(page.hasNext);
      },
      setError,
      (isLoading) => {
        loadingRef.current = isLoading;
        setLoading(isLoading);
      }
    )
  }, [pagePath, toPage, setError])

  useEffect(() => {
    loadNextPage();
    // Only meant to run once on mount, loadNextPage advances its own page/hasNext refs
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        loadNextPage();
      }
    });
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [loadNextPage])

  return { items, setItems, hasNext, loading, sentinelRef };
}
