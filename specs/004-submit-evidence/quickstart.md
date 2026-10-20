# Quickstart: I3 — Recopilación y remisión de evidencias

## Purpose

Validate the complete synthetic journey required by SC-001: submit v1 with a file, prove that v1 cannot change, create and submit v2, and recover the complete history. The same run also checks file validation, authorization, concurrency, recovery and audit without invoking document review, academic evaluation, progress or process closing.

## Prerequisites

- Java 25, Maven 3.9, Node supported by `frontend/package.json`, PostgreSQL 18, PowerShell, the pinned Playwright browser dependencies and the documented k6 version.
- I0 acceptance data: synthetic responsible user with a current assignment and a second out-of-scope user.
- I1/I2 acceptance data: one published synthetic model, one active synthetic process and at least two indicator snapshots assigned to the responsible user.
- Writable local file root outside PostgreSQL, configured with `SEC_FILE_ROOT`.
- Synthetic fixtures under `tests/fixtures/pilot/evidence/`; no institutional files, identities, URIs or secrets.
- The implementation-specific migration order through `V007__evidence_immutability.sql` is available from a clean database.

The integrated scenario must stop if I0–I2 are not operational. Domain and storage test suites may run earlier using explicit doubles, but I3 must not invent replacement identity/process rules.

## 1. Validate the contract

From the repository root:

```powershell
frontend\node_modules\.bin\redocly.cmd lint specs\004-submit-evidence\contracts\openapi.yaml
postman.cmd spec lint specs\004-submit-evidence\contracts\openapi.yaml --issueType syntax --no-report-events
```

Expected: both commands report a valid OpenAPI 3.1 document with no syntax issues. Use `postman.cmd` on Windows because the PowerShell wrapper can be blocked by execution policy. Postman may warn that its user-level log directory is unavailable in a restricted sandbox; this does not invalidate a successful lint result.

Before merging implementation, reconcile the approved operations and schemas into `docs/technical/contracts/openapi.yaml` and run:

```powershell
frontend\node_modules\.bin\redocly.cmd lint docs\technical\contracts\openapi.yaml
node scripts\validate-contracts.mjs
```

## 2. Run clean migration and automated suites

```powershell
Set-Location backend
.\mvnw.cmd verify
Set-Location ..\frontend
npm test
npm run lint
npm run build
npx playwright test ..\tests\acceptance
Set-Location ..
k6 run tests\performance\evidence-upload.js
```

Expected:

- Flyway migrates a clean PostgreSQL database and Hibernate validation succeeds.
- Domain tests prove draft/submitted invariants, succession and concurrency.
- Storage tests cover 0 bytes according to policy, exactly 26 214 400 bytes, 26 214 401 bytes, MIME mismatch, SHA-256 and every interruption window.
- Contract/security tests cover ETag, `If-Match`, RFC 9457, hidden out-of-scope IDs and safe audit fields.
- Angular tests cover editor state, upload progress, stale ETag, immutable detail, successor action, keyboard focus and error correlation.
- Playwright executes the synthetic browser journeys and k6 produces the documented p95 measurements.
- Existing I0–I2 regression tests remain green.

## 3. Start the reference environment without Docker

Use a local PostgreSQL instance and two terminals:

```powershell
$env:SEC_DB_URL='jdbc:postgresql://localhost:5432/sec_unl'
$env:SEC_DB_USER='sec_app'
$env:SEC_DB_PASSWORD='<local-development-secret>'
$env:SEC_FILE_ROOT=(Resolve-Path '.\var\files').Path
Set-Location backend
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location frontend
npm start
```

Do not commit the local password, generated files or runtime directories. Docker may be used optionally but is not the only validation path.

## 4. Prepare synthetic acceptance data

Load only the repository fixture manifest. It must identify:

- `responsible-in-scope` with an active responsibility over `indicator-a` and `indicator-b`;
- `responsible-out-of-scope` for negative download/direct-ID tests;
- active `process-i3` fixed to the published model version containing those indicators;
- a small permitted synthetic file with known SHA-256;
- an exact 25 MiB synthetic file and a 25 MiB + 1 byte synthetic file;
- a misleading-extension/type-mismatch file;
- a safe example-domain external URI.

Run the repository data-protection check before using the fixtures:

```powershell
.\scripts\verify-synthetic-data.ps1
```

Expected: no real institutional data or secrets are detected.

## 5. Primary v1 journey

Use the Angular UI or the equivalent operations in [contracts/openapi.yaml](./contracts/openapi.yaml):

1. Sign in as `responsible-in-scope`.
2. Create an evidence for `process-i3` with title, description, synthetic provenance, inclusive covered dates and both assigned indicators.
3. Capture the returned v1 draft ID and strong ETag.
4. Upload the permitted synthetic file using that ETag.
5. Verify the response records distinct declared/detected types, exact byte size, lowercase SHA-256 and `AVAILABLE`, without exposing a storage key.
6. Add a described external reference and confirm files/references are displayed separately.
7. Submit v1 using the latest ETag.

Expected:

- v1 returns `SUBMITTED`, revision 1, null predecessor, author and submission time.
- no ETag for further editing is offered.
- the audit query contains one correlated successful submission event without file bytes, storage keys or full sensitive URI data.
- no document-review or academic outcome is created.

External references are validated structurally only. The server must not contact, resolve or probe their destinations during this scenario.

## 6. Immutability and authorized download

1. Attempt `PATCH`, file upload, file removal and external-reference mutation against submitted v1.
2. Download the v1 file as the in-scope responsible user and calculate SHA-256 locally.
3. Repeat the direct download using the out-of-scope identity and a known v1/file ID.

Expected:

- every mutation of v1 fails with a stable Problem Detail and v1 remains byte-for-byte/field-for-field unchanged;
- the authorized download hash equals the stored snapshot;
- the out-of-scope request returns the same not-found shape as an unknown ID, with no bytes or protected metadata;
- the denied download is safely audited.

## 7. Successor v2 and complete history

1. Invoke “Create successor” on submitted v1.
2. Verify v2 is a `DRAFT`, revision 2, `predecessorId = v1.id`, and initially copies the exact v1 content/sources.
3. Edit a descriptive field and replace or add a synthetic source using the returned ETag.
4. Submit v2.
5. Open the evidence history.

Expected:

- v2 becomes immutable and points directly to v1;
- v1 remains identical to the snapshot captured in step 5;
- history contains exactly v1 and v2 in unambiguous order with their own authors, dates, metadata, indicators, files/references and hashes;
- creating a second direct successor from v1 returns `409 SUCCESSOR_ALREADY_EXISTS`.

## 8. Boundary and concurrency matrix

Execute the automated/API cases below:

| Case | Expected result |
|---|---|
| Exactly 26 214 400 bytes, permitted detected type | Accepted within 15 s p95 in reference environment |
| 26 214 401 bytes | `413 FILE_TOO_LARGE`; no usable association |
| Detected type forbidden | `422 FILE_TYPE_NOT_ALLOWED` |
| Declared/detected mismatch forbidden by policy | `422 FILE_TYPE_MISMATCH` |
| Zero-byte file | Result follows the active synthetic policy, not a hard-coded invariant |
| Duplicate SHA-256 | Separate identities/associations; no cross-scope authorization |
| Two `PATCH` requests with the same ETag | One succeeds; one returns `412 STALE_DRAFT` |
| Mutation without `If-Match` | `428 PRECONDITION_REQUIRED` |
| Two submissions of the same draft | At most one submitted revision |
| Two successor requests for the same predecessor | At most one successor |
| Indicator outside model/process/scope | Safe rejection; no association and no information leak |

## 9. Recovery matrix

Run fault-injection tests at each boundary and then the reconciler:

| Injected interruption | Required observable result |
|---|---|
| During staging | No usable file; expired partial remains safely identifiable |
| After staging before DB record | Orphan staging object discovered after grace period |
| After DB `STAGED` before promotion | Promotion retried idempotently |
| After promotion before DB `AVAILABLE` | Final object verified and reconciled; never prematurely visible |
| DB says `AVAILABLE`, object absent | State becomes `MISSING`; submission/download blocked |
| Final bytes do not match SHA-256 | State becomes `QUARANTINED`; no bytes delivered |
| Submission audit write fails | No `SUBMITTED` version is committed |

Expected: SC-003 holds in 100% of simulated interruptions. Every failure has a safe correlation and audit event, and no recovery path deletes a submitted historical object.

## 10. Backup/restore and final evidence

Restore a clean database backup together with the matching file-root backup, then run reconciliation and repeat one authorized download from v1 and v2. Record:

- automated test reports;
- contract lint output (including Postman CLI);
- migration-from-clean result;
- recovery matrix result;
- p95 upload measurement;
- synthetic-data scan;
- screenshots or logs for v1 immutability and v2 history;
- updated traceability links for FR-016–FR-020, RNF-004/005/010/011/019 and SC-001–SC-008.

The feature is not accepted while a critical requirement, constitutional gate, real-data finding or unreconciled false file reference remains.

## 11. Usability outcome SC-007

Run the same synthetic create → attach → submit journey with 20 representative responsible-user participants who have not rehearsed the task. Record participant code, start/end time, first-attempt completion, help requested and result without collecting real institutional evidence.

Expected: at least 19 of 20 participants finish successfully on the first attempt in under five minutes using only the interface and user guidance. Store the anonymized protocol and aggregate result in `tests/acceptance/results/evidence-usability.md`.
