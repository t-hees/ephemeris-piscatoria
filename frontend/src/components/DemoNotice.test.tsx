import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import DemoNotice from './DemoNotice';
import { LanguageProvider } from '../i18n/LanguageProvider';
import { translations } from '../i18n/LanguageContext';

const t = translations.en;

function renderNotice(path = "/") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <LanguageProvider>
        <DemoNotice />
      </LanguageProvider>
    </MemoryRouter>
  );
}

describe("DemoNotice", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(window.navigator, 'languages', 'get').mockReturnValue(['en-US']);
  });

  it("is shown until accepted and stays hidden afterwards", async () => {
    const { unmount } = renderNotice();
    expect(screen.getByRole("dialog", { name: t.demoNotice.title })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: t.demoNotice.accept }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    unmount();
    renderNotice();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("can switch the language", async () => {
    renderNotice();
    await userEvent.click(screen.getByRole("button", { name: "DE" }));

    expect(screen.getByRole("dialog", { name: translations.de.demoNotice.title })).toBeInTheDocument();
  });

  it("doesn't cover the legal pages", () => {
    renderNotice("/privacy");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
