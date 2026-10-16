import { expect, test } from '@playwright/test';
import { createTrip, editTrip, field, fillNewTrip, login, loginAsAdmin, register, registerAndLogin, saveTrip, selectFish, tripCard, uniqueUser } from './helpers';

const onePixelPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');

test('register and login', async ({ page }) => {
  const user = uniqueUser();
  await register(page, user);
  await login(page, user);

  await expect(page.getByRole('button', { name: user.username })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Neuer Angelausflug' })).toBeVisible();
});

test('login with wrong password shows an error', async ({ page }) => {
  const user = uniqueUser();
  await register(page, user);
  await login(page, { ...user, password: 'wrong' });

  await expect(page.getByText(/^AUTH_FAIL: /)).toBeVisible();
});

test('create a trip', async ({ page }) => {
  await registerAndLogin(page);
  await createTrip(page, 'Cool Lake', '2026-04-05T06:30');

  const card = tripCard(page, 'Cool Lake');
  await expect(card.locator('.trip-date')).toHaveText('2026-04-05');
  await card.locator('.trip-card-header').click();
  await expect(card.getByText('3 Stunden')).toBeVisible();
  await expect(card.getByText('Noch keine Fänge erfasst.')).toBeVisible();
});

test('create a trip together with its catches', async ({ page }) => {
  await registerAndLogin(page);
  await fillNewTrip(page, 'Catch Lake', '2026-05-01T08:00');
  await selectFish(page, 0, 'Aalmutter', 'Aalmutter (Zoarces viviparus)');
  await page.locator('.quantity-row').getByRole('button', { name: '+' }).click();
  await selectFish(page, 1, 'Meeraal', 'Meeraal (Conger conger)');
  await field(page, 'Größe').fill('80');
  await saveTrip(page, 'Catch Lake');

  const card = tripCard(page, 'Catch Lake');
  await card.locator('.trip-card-header').click();
  const simpleCatch = card.locator('.simple-catch-table tr');
  await expect(simpleCatch.locator('td').nth(0)).toHaveText('Aalmutter (Zoarces viviparus)');
  await expect(simpleCatch.locator('td').nth(1)).toHaveText('2');
  await expect(card.locator('.entry-list-item h3')).toHaveText('Meeraal (Conger conger)');
  await expect(card.getByText('Größe: 80')).toBeVisible();
});

test('edit a trip and its catches', async ({ page }) => {
  await registerAndLogin(page);
  await fillNewTrip(page, 'Old Lake', '2026-05-01T08:00');
  await selectFish(page, 0, 'Aalmutter', 'Aalmutter (Zoarces viviparus)');
  await selectFish(page, 1, 'Meeraal', 'Meeraal (Conger conger)');
  await saveTrip(page, 'Old Lake');
  await editTrip(page, 'Old Lake');

  // Opened by its own url, which also works when loaded directly
  await expect(page).toHaveURL(/\/trips\/\d+\/edit$/);
  await page.reload();
  await expect(field(page, 'Ort:')).toHaveValue('Old Lake');
  await field(page, 'Ort:').fill('New Lake');
  await page.getByRole('button', { name: 'Fisch löschen' }).click();
  await saveTrip(page, 'New Lake');

  await expect(tripCard(page, 'Old Lake')).toHaveCount(0);
  const card = tripCard(page, 'New Lake');
  await card.locator('.trip-card-header').click();
  await expect(card.locator('.simple-catch-table td').nth(0)).toHaveText('Aalmutter (Zoarces viviparus)');
  await expect(card.locator('.entry-list-item')).toHaveCount(0);
});

test('upload a photo of a catch as admin', async ({ page }) => {
  await loginAsAdmin(page);
  // The admin account outlives a single run, so its trips need unique names
  const location = `Photo Lake ${Date.now()}`;
  await createTrip(page, location, '2026-05-02T08:00');
  await editTrip(page, location);

  await selectFish(page, 1, 'Meeraal', 'Meeraal (Conger conger)');
  await page.locator('input[type=file]').setInputFiles({ name: 'photo.png', mimeType: 'image/png', buffer: onePixelPng });
  await saveTrip(page, location);

  // Loaded from the api with the auth cookie, a failed request would leave the image without size
  const card = tripCard(page, location);
  await card.locator('.trip-card-header').click();
  const photo = card.getByAltText('Foto Meeraal (Conger conger)');
  await expect(photo).toBeVisible();
  await expect.poll(() => photo.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBe(1);
});

test('regular users cannot upload photos', async ({ page }) => {
  await registerAndLogin(page);
  await fillNewTrip(page, 'No Photo Lake', '2026-05-03T08:00');
  await selectFish(page, 1, 'Meeraal', 'Meeraal (Conger conger)');
  await expect(page.getByText('Das Hochladen von Fotos ist in der Demo deaktiviert.')).toBeVisible();
  await expect(page.locator('input[type=file]')).toHaveCount(0);
});

test('search filters the trip list', async ({ page }) => {
  await registerAndLogin(page);
  await createTrip(page, 'River Bend', '2026-06-01T09:00', 'RIVER');
  await createTrip(page, 'Ocean Pier', '2026-07-15T10:00', 'OCEAN');

  await page.getByPlaceholder('Ort suchen...').fill('river');
  await expect(tripCard(page, 'River Bend')).toBeVisible();
  await expect(tripCard(page, 'Ocean Pier')).toHaveCount(0);

  await page.getByPlaceholder('Ort suchen...').fill('');
  await page.locator('.trip-search-bar select').selectOption('OCEAN');
  await expect(tripCard(page, 'Ocean Pier')).toBeVisible();
  await expect(tripCard(page, 'River Bend')).toHaveCount(0);
});

test('trips of other users are not visible', async ({ page, browser }) => {
  await registerAndLogin(page);
  await createTrip(page, 'Secret Spot', '2026-06-01T09:00');

  const otherPage = await (await browser.newContext()).newPage();
  await registerAndLogin(otherPage);
  await expect(otherPage.getByRole('button', { name: 'Neuer Angelausflug' })).toBeVisible();
  await expect(tripCard(otherPage, 'Secret Spot')).toHaveCount(0);
});

test('logout and delete account', async ({ page }) => {
  const user = await registerAndLogin(page);

  await page.getByRole('button', { name: 'LOGOUT' }).click();
  await expect(page.getByRole('button', { name: 'LOGIN' })).toBeVisible();
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();

  await login(page, user);
  await page.getByRole('button', { name: user.username }).click();
  await page.getByRole('button', { name: 'Account löschen' }).click();
  await page.locator('input[type=password]').fill(user.password);
  await page.getByRole('button', { name: 'Senden' }).click();
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible();

  await login(page, user);
  await expect(page.getByText(/^AUTH_FAIL: /)).toBeVisible();
});
