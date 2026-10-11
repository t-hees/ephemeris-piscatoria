import { expect, type Locator, type Page } from '@playwright/test';

export type TestUser = { username: string, password: string };

export function uniqueUser(): TestUser {
  return {
    username: `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    password: 'e2e-password',
  };
}

/**
 * Form inputs aren't associated with their labels, so locate them as the element following the label
 */
export function field(page: Page, label: string, element: 'input' | 'select' = 'input'): Locator {
  return page.locator(`label:text-is("${label}") + ${element}`);
}

export async function register(page: Page, user: TestUser) {
  await page.goto('/');
  await page.getByRole('button', { name: 'Zu Registrieren wechseln' }).click();
  await field(page, 'Benutzername:').fill(user.username);
  await field(page, 'Passwort:').fill(user.password);
  await field(page, 'Wiederhole Passwort:').fill(user.password);
  // The test site key solves the captcha on its own, the form needs the token before sending
  await expect(page.locator('input[name="cf-turnstile-response"]')).toHaveValue(/.+/);
  await page.getByRole('button', { name: 'Senden' }).click();
  await expect(page.getByText('Registrierung erfolgreich! Versuche Login')).toBeVisible();
}

export async function login(page: Page, user: TestUser) {
  await field(page, 'Benutzername:').fill(user.username);
  await field(page, 'Passwort:').fill(user.password);
  await page.getByRole('button', { name: 'Senden' }).click();
}

export async function registerAndLogin(page: Page): Promise<TestUser> {
  const user = uniqueUser();
  await register(page, user);
  await login(page, user);
  await expect(page.getByRole('button', { name: user.username })).toBeVisible();
  return user;
}

// Created by the backend on startup from ADMIN_* in docker-compose.e2e.yml, the only user that may upload images
export const admin: TestUser = { username: 'e2e-admin', password: 'e2e-admin-password' };

export async function loginAsAdmin(page: Page) {
  await page.goto('/');
  await login(page, admin);
  await expect(page.getByRole('button', { name: admin.username })).toBeVisible();
}

export async function createTrip(page: Page, location: string, time: string, environment = 'LAKE') {
  await page.getByRole('button', { name: 'Neuer Angelausflug' }).click();
  await field(page, 'Tag/Uhrzeit:').fill(time);
  await field(page, 'Ort:').fill(location);
  await field(page, 'Gewässerart:', 'select').selectOption(environment);
  await field(page, 'Dauer(in h):').fill('3');
  await page.getByRole('button', { name: 'Angelausflug erstellen' }).click();
  await expect(tripCard(page, location)).toBeVisible();
}

export function tripCard(page: Page, location: string): Locator {
  return page.locator('.trip-card').filter({ has: page.locator('.trip-location', { hasText: location }) });
}

export async function selectFish(page: Page, searchIndex: number, query: string, result: string) {
  await page.getByPlaceholder('Suchbegriff eingeben...').nth(searchIndex).fill(query);
  await page.getByText(result, { exact: true }).click();
}
