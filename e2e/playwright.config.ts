import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:3040';

/**
 * Runs against the stack from docker-compose.e2e.yml, see README.org for how to start it
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  // Journeys share one backend, every test registers its own user to stay independent
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }], ['junit', { outputFile: 'reports/junit.xml' }]]
    : 'list',
  use: {
    baseURL,
    // The demo notice would cover every page, only the journey testing it starts without
    storageState: {
      cookies: [],
      origins: [{ origin: new URL(baseURL).origin, localStorage: [{ name: 'demoNoticeAccepted', value: 'true' }] }],
    },
    trace: 'retain-on-failure',
    // The ui language follows the browser, the journeys use the german texts
    locale: 'de-DE',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
