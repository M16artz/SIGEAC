import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: '../tests/acceptance',
  outputDir: '../tests/acceptance/results/playwright-artifacts',
  reporter: [['list'], ['html', { outputFolder: '../tests/acceptance/results/playwright-report', open: 'never' }]],
  retries: 0,
  use: {
    baseURL: process.env['SEC_WEB_BASE_URL'] ?? 'http://127.0.0.1:4200',
    trace: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ]
});
