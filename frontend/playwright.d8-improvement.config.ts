import {defineConfig, devices} from '@playwright/test';
export default defineConfig({
  testDir: '../tests/acceptance', testMatch: ['improvement.spec.ts'],
  workers: 1, timeout: 180000, outputDir: '../tests/acceptance/results/playwright-artifacts/d8-improvement', reporter: 'list',
  use: {...devices['Desktop Chrome'], channel: process.env['SEC_BROWSER_CHANNEL'] ?? 'msedge',
    baseURL: process.env['SEC_WEB_BASE_URL'] ?? 'http://127.0.0.1:14206', headless: true, trace: 'retain-on-failure'}
});
