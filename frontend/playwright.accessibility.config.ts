import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: '../tests/e2e', testMatch: 'accessibility-complete-workflows.spec.ts',
  workers: 1, timeout: 60000, reporter: 'list',
  outputDir: '../tests/acceptance/results/playwright-artifacts/accessibility',
  use: {...devices['Desktop Chrome'], channel: process.env['SEC_BROWSER_CHANNEL'] ?? 'msedge',
    baseURL: process.env['SEC_WEB_BASE_URL'] ?? 'http://127.0.0.1:14206',
    headless: true, trace: 'retain-on-failure'}
});
