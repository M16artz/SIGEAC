import {defineConfig} from '@playwright/test';

export default defineConfig({
  testDir: '../tests/acceptance', testMatch: 'complete-workflows.spec.ts',
  workers: 1, timeout: 90000,
  outputDir: '../tests/acceptance/results/playwright-artifacts/complete-workflows',
  reporter: [['list'], ['json', {outputFile: '../backend/target/complete-workflows-playwright.json'}]],
  use: {baseURL: process.env['SEC_ACCEPTANCE_API_ORIGIN'] ?? 'http://127.0.0.1:18087'}
});
