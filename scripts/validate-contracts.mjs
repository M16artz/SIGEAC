import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const scriptsDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptsDir, '..');
const binDir = path.join(root, 'frontend', 'node_modules', '.bin');
const executable = (name) => path.join(binDir, process.platform === 'win32' ? `${name}.cmd` : name);
const commands = [
  [executable('redocly'), ['lint', path.join(root, 'docs', 'technical', 'contracts', 'openapi.yaml')]],
  [executable('asyncapi'), ['validate', path.join(root, 'docs', 'technical', 'contracts', 'events-asyncapi.yaml')]]
];
for (const [command, args] of commands) {
  const environment = {...process.env, SUPPRESS_NO_CONFIG_WARNING: 'true'};
  const result = process.platform === 'win32'
    ? spawnSync(process.env.ComSpec ?? 'cmd.exe', ['/d', '/s', '/c', command, ...args], {cwd: root, stdio: 'inherit', env: environment})
    : spawnSync(command, args, {cwd: root, stdio: 'inherit', env: environment});
  if (result.status !== 0) process.exit(result.status ?? 1);
}
