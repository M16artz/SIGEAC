import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const frontend = path.join(root, 'frontend');
const result = spawnSync(process.execPath, [path.join(frontend, 'node_modules/@playwright/test/cli.js'),
  'test', '--config=playwright.d8-archive.config.ts', ...process.argv.slice(2)], {cwd: frontend, stdio: 'inherit',
  env: {...process.env, NODE_PATH: [path.join(frontend, 'node_modules'), process.env.NODE_PATH].filter(Boolean).join(path.delimiter)}});
if (result.error) throw result.error;
process.exit(result.status ?? 1);
