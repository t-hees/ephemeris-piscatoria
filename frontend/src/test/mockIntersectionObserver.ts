import { vi } from 'vitest';

/**
 * Replaces IntersectionObserver (missing in jsdom) with one that only reports an intersection when the
 * returned function is called
 */
export function mockIntersectionObserver(): () => void {
  let callbacks: IntersectionObserverCallback[] = [];
  vi.stubGlobal("IntersectionObserver", class {
    callback: IntersectionObserverCallback;
    constructor(callback: IntersectionObserverCallback) { this.callback = callback; }
    observe() { callbacks.push(this.callback); }
    disconnect() { callbacks = callbacks.filter(callback => callback !== this.callback); }
  });
  return () => callbacks.forEach(callback =>
    callback([{ isIntersecting: true } as IntersectionObserverEntry], {} as IntersectionObserver));
}
