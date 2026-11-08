import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const scriptsDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptsDir, '..');
const moduleDir = path.join(root, 'frontend', 'node_modules');
const redocly = path.join(moduleDir, '@redocly', 'cli', 'bin', 'cli.js');
const asyncapi = path.join(moduleDir, '@asyncapi', 'cli', 'bin', 'run_bin');
const globalContract = path.join(root, 'docs', 'technical', 'contracts', 'openapi.yaml');
const evidenceContract = path.join(root, 'specs', '004-submit-evidence', 'contracts', 'openapi.yaml');
const commands = [
  [process.execPath, [redocly, 'lint', globalContract]],
  ...['001-identity-access-scopes', '002-version-evaluation-model', '003-manage-self-assessment']
    .map(feature => [process.execPath, [redocly, 'lint', path.join(root, 'specs', feature, 'contracts', 'openapi.yaml')]]),
  [process.execPath, [redocly, 'lint', evidenceContract]],
  [process.execPath, [redocly, 'lint', path.join(root,'specs','005-complete-evaluation-workflows','contracts','openapi.yaml')]],
  [process.execPath, [path.join(root,'scripts','check-contract-compatibility.mjs')]],
  [process.execPath, [asyncapi, 'validate', path.join(root, 'docs', 'technical', 'contracts', 'events-asyncapi.yaml')]]
];
for (const [command, args] of commands) {
  const environment = {...process.env, CI: 'true', REDOCLY_TELEMETRY: 'off', SUPPRESS_NO_CONFIG_WARNING: 'true'};
  const result = spawnSync(command, args, {cwd: root, stdio: 'inherit', env: environment});
  if (result.status !== 0) process.exit(result.status ?? 1);
}
