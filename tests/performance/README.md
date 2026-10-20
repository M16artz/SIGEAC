# Reproducible I3 performance checks

The I3 load runner is k6 **1.1.0**. Install that exact release from the
official Grafana k6 distribution for the host platform, then verify it with
`k6 version`. Docker is not required or assumed.

From `frontend/`, run `npm run test:performance`. The script executes
`k6 run ../tests/performance/evidence-upload.js` and must use synthetic files
only. The acceptance thresholds are p95 <= 15 seconds for upload/rejection of
files up to 25 MiB and p95 <= 2 seconds for interactive operations without
file transfer at the documented pilot concurrency.

Playwright is pinned to **1.64.1** in `frontend/package.json`. Install its
pinned Chromium and Firefox binaries from `frontend/` with
`npx playwright install chromium firefox`, then run `npm run test:acceptance`.
