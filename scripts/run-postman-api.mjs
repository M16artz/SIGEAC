import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const cli = process.env.SEC_POSTMAN_CLI ?? path.join(path.dirname(process.execPath), 'node_modules/postman-cli/bin/postman.js');
if (!existsSync(cli)) {
  throw new Error('Define SEC_POSTMAN_CLI con la ruta a postman-cli/bin/postman.js de tu instalación de Postman CLI.');
}
const environment = JSON.parse(readFileSync(path.join(root, 'tests/postman/local.postman_environment.json'), 'utf8'));
const baseUrl = (process.env.SEC_API_BASE_URL ?? environment.values.find(value => value.key === 'baseUrl').value).replace(/\/+$/, '');
const overrides = {
  baseUrl,
  baseOrigin: new URL(baseUrl).origin,
  username: process.env.SEC_API_USERNAME,
  password: process.env.SEC_API_PASSWORD
};
for (const value of environment.values) {
  if (overrides[value.key] !== undefined) value.value = overrides[value.key];
}
const output = path.join(root, 'backend/target/postman-api');
mkdirSync(output, { recursive: true });
const temporary = mkdtempSync(path.join(tmpdir(), 'sigeac-postman-'));
const environmentFile = path.join(temporary, 'environment.json');
try {
  writeFileSync(environmentFile, JSON.stringify(environment), { mode: 0o600 });
  const result = spawnSync(process.execPath, [cli, 'collection', 'run',
    path.join(root, 'tests/postman/sigeac.postman_collection.json'),
    '--environment', environmentFile,
    '--no-report-events', '--reporters', 'cli,junit',
    '--reporter-junit-export', path.join(output, 'results.xml'),
    '--timeout-request', '15000', '--timeout', '120000'
  ], { cwd: root, stdio: 'inherit', env: process.env });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  rmSync(environmentFile, { force: true });
  rmdirSync(temporary);
}
