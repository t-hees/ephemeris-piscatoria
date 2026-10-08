import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Register } from './Register';
import { jsonResponse, mockFetch, requestBodies } from '../test/mockFetch';
import { translations } from '../i18n/LanguageContext';

const t = translations.de;

function renderRegister(siteKey: string | null) {
  const fetchMock = mockFetch({
    "GET auth/captcha": () => jsonResponse({ siteKey }),
    "POST auth/register": () => jsonResponse({ username: "john" }, 201),
  });
  const setNotification = vi.fn();
  const setError = vi.fn();
  render(<Register setNotification={setNotification} setError={setError} />);
  return { fetchMock, setNotification, setError };
}

async function fillForm() {
  const [username, password, repeat] = [
    document.querySelector('input[type=text]')!,
    ...document.querySelectorAll('input[type=password]'),
  ];
  await userEvent.type(username, "john");
  await userEvent.type(password, "strongpass");
  await userEvent.type(repeat, "strongpass");
}

describe("Register", () => {
  it("registers without captcha when none is configured", async () => {
    const { fetchMock, setNotification } = renderRegister(null);
    await fillForm();

    await userEvent.click(screen.getByText(t.common.send));

    await vi.waitFor(() => expect(setNotification).toHaveBeenCalledWith(t.loginPage.registerSuccess));
    expect(requestBodies(fetchMock, "auth/register", "POST")).toEqual([
      { username: "john", password: "strongpass", captchaToken: null },
    ]);
    expect(document.querySelector('script[src*="challenges.cloudflare.com"]')).toBeNull();
  });

  it("requires the captcha when a site key is configured", async () => {
    const { fetchMock, setError } = renderRegister("sitekey");
    // Loading the real widget script isn't possible in jsdom, the form has to wait for a token
    await vi.waitFor(() => expect(document.querySelector('script[src*="challenges.cloudflare.com"]')).not.toBeNull());
    await fillForm();

    await userEvent.click(screen.getByText(t.common.send));

    expect(setError).toHaveBeenCalledWith(t.loginPage.captchaMissing);
    expect(requestBodies(fetchMock, "auth/register", "POST")).toHaveLength(0);
  });
});
