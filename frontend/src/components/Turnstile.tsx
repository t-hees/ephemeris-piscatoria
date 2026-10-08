import { useEffect, useRef } from "react";

type TurnstileOptions = {
  sitekey: string,
  callback: (token: string) => void,
  "expired-callback": () => void,
  "error-callback": () => void,
};

type TurnstileApi = {
  render: (container: HTMLElement, options: TurnstileOptions) => string,
  reset: (widgetId: string) => void,
  remove: (widgetId: string) => void,
};

declare global {
  interface Window {
    turnstile?: TurnstileApi,
  }
}

const scriptUrl = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
let scriptPromise: Promise<TurnstileApi> | null = null;

// The script is only loaded once a captcha is actually shown, so other pages don't contact Cloudflare
function loadTurnstile(): Promise<TurnstileApi> {
  if (window.turnstile) return Promise.resolve(window.turnstile);
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = scriptUrl;
      script.async = true;
      script.onload = () => window.turnstile ? resolve(window.turnstile) : reject(new Error("Turnstile missing"));
      script.onerror = () => {
        scriptPromise = null;
        reject(new Error("Failed to load captcha"));
      };
      document.head.appendChild(script);
    });
  }
  return scriptPromise;
}

type TurnstileProps = {
  siteKey: string,
  // null when the token expired or the challenge failed
  onToken: (token: string | null) => void,
  // Tokens can only be verified once, a change of this value requests a new one
  resetSignal: number,
  onError: (message: string) => void,
};

/**
 * Cloudflare Turnstile captcha, see CaptchaService in the backend
 */
export function Turnstile({ siteKey, onToken, resetSignal, onError }: TurnstileProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const widgetIdRef = useRef<string | null>(null);
  // The callbacks given to turnstile on render always call the latest props
  const onTokenRef = useRef(onToken);
  const onErrorRef = useRef(onError);
  useEffect(() => {
    onTokenRef.current = onToken;
    onErrorRef.current = onError;
  });

  useEffect(() => {
    let cancelled = false;
    loadTurnstile()
      .then(turnstile => {
        if (cancelled || !containerRef.current) return;
        widgetIdRef.current = turnstile.render(containerRef.current, {
          sitekey: siteKey,
          callback: token => onTokenRef.current(token),
          "expired-callback": () => onTokenRef.current(null),
          "error-callback": () => onTokenRef.current(null),
        });
      })
      .catch(err => onErrorRef.current(err.message));
    return () => {
      cancelled = true;
      if (widgetIdRef.current) window.turnstile?.remove(widgetIdRef.current);
      widgetIdRef.current = null;
    };
  }, [siteKey]);

  useEffect(() => {
    if (resetSignal > 0 && widgetIdRef.current) {
      window.turnstile?.reset(widgetIdRef.current);
      onTokenRef.current(null);
    }
  }, [resetSignal]);

  return <div ref={containerRef} />;
}
